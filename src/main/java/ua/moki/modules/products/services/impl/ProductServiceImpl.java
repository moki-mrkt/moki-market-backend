package ua.moki.modules.products.services.impl;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.infrastructure.storage.service.FileStorageService;
import ua.moki.modules.orders.repositories.OrderItemRepository;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.domains.ProductImage;
import ua.moki.modules.products.dtos.ProductRequestDTO;
import ua.moki.modules.products.dtos.ProductResponseDTO;
import ua.moki.modules.products.dtos.ProductSearchRequestDTO;
import ua.moki.modules.products.dtos.SearchResponse;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.repositories.ProductRepository;
import ua.moki.modules.products.services.ProductService;
import ua.moki.modules.products.services.ProductSpecifications;
import ua.moki.modules.products.utils.mappers.ProductMapper;
import ua.moki.util.exceptions.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private final Clock clock;
    private final ProductMapper productMapper;
    private final FileStorageService fileStorageService;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductSpecifications productSpecifications;

    @Autowired
    public ProductServiceImpl(Clock clock,
                              ProductMapper productMapper,
                              FileStorageService fileStorageService,
                              OrderItemRepository orderItemRepository,
                              ProductRepository productRepository,
                              ProductSpecifications productSpecifications) {
        this.clock = clock;
        this.productMapper = productMapper;
        this.fileStorageService = fileStorageService;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.productSpecifications = productSpecifications;
    }


    @Override
    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO productRequestDTO) {

        Product product = productMapper.toEntity(productRequestDTO);

        product.setCreationTime(OffsetDateTime.now(clock));
        product.setRating(BigDecimal.valueOf(0));

        Product savedProduct = productRepository.save(product);
        // Todo create for elasticsearch
        // productSearchRepository.save(productMapper.toDocument(savedProduct));

        log.info("product created: {}", savedProduct.getId());

        return productMapper.toResponseDTO(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponseDTO updateProduct(Long productId, ProductRequestDTO productRequestDTO) {

        Product product = findById(productId);

        productMapper.updateEntityFromDto(productRequestDTO, product);

        Product savedProduct = productRepository.save(product);

        // Todo update for elasticsearch
        // productSearchRepository.save(productMapper.toDocument(savedProduct));

        return productMapper.toResponseDTO(savedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {

        Product product = findById(productId);

        if (orderItemRepository.existsByProductId(productId)) {

            product.setAvailability(ProductAvailability.ARCHIVED);
            productRepository.save(product);
        } else {

            log.info("Product {} has NO orders. Performing hard delete.", productId);

            List<String> imageKeys = product.getImages().stream()
                    .map(ProductImage::getImageId)
                    .toList();

            productRepository.delete(product);

            // 3. Чистимо S3 (асинхронно або тут же, якщо ключів мало)
            if (!imageKeys.isEmpty()) fileStorageService.deleteAllFiles(imageKeys);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Product findById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(Long productId) {
        return productMapper.toResponseDTO(findById(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getAllProducts(String query, Pageable pageable) {

        if (query == null || query.isBlank()) {
            return productRepository.findAll(pageable)
                    .map(productMapper::toResponseDTO);
        }

        ProductSearchRequestDTO searchRequest = new ProductSearchRequestDTO(
                query, null, null, null, null, null
        );

        Specification<Product> spec = productSpecifications.getSpecifications(searchRequest, false);

        return productRepository.findAll(spec, pageable)
                .map(productMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getAllProductByCategory(ProductCategory productCategory, int page, int size) {
        return productRepository.findAllByProductCategory(productCategory, PageRequest.of(page, size)).map(productMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getNewProducts(int page, int size) {

        return productRepository.findAll(PageRequest.of(page, size, Sort.by("creationTime").descending()))
                .map(productMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getProductsWithDiscount(int page, int size) {
        return productRepository.findAllWithDiscount(PageRequest.of(page, size, Sort.by("creationTime")))
                .map(productMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getBestsellers(int page, int size) {
        return productRepository.findAll(PageRequest.of(page, size, Sort.by("salesCount").descending()))
                .map(productMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResponse searchProducts(ProductSearchRequestDTO request, Pageable pageable) {

        Specification<Product> productSpec = productSpecifications.getSpecifications(request, false);
        Specification<Product> facetSpec = productSpecifications.getSpecifications(request, true);


        Page<Product> products = productRepository.findAll(productSpec, pageable);
        List<String> subcategories = productRepository.findDistinctSubcategoriesBySpec(facetSpec);

        ProductSearchRequestDTO limitRequest = new ProductSearchRequestDTO(request.query(), request.category(),
                null, null,
                request.subcategory(), request.hasDiscount());

        Specification<Product> limitSpec = productSpecifications.getSpecifications(limitRequest, false);

        Map<String, Double> minMax = productSpecifications.getMinMaxPricesBySpecification(limitSpec);

        return new SearchResponse(
                products.map(productMapper::toResponseDTO),
                subcategories,
                minMax.get("minPrice"),
                minMax.get("maxPrice")
        );
    }
}
