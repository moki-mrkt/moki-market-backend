package ua.moki.modules.products.services.impl;


import com.github.slugify.Slugify;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.infrastructure.storage.service.FileStorageService;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.domains.ProductImage;
import ua.moki.modules.products.dtos.ProductRequestDTO;
import ua.moki.modules.products.dtos.ProductResponseDTO;
import ua.moki.modules.products.dtos.ProductSearchRequestDTO;
import ua.moki.modules.products.dtos.SearchResponse;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.repositories.FavoriteProductRepository;
import ua.moki.modules.products.repositories.ProductRepository;
import ua.moki.modules.products.services.ProductService;
import ua.moki.modules.products.services.ProductSpecifications;
import ua.moki.modules.products.utils.mappers.ProductMapper;
import ua.moki.util.exceptions.AlreadyExistsException;
import ua.moki.util.exceptions.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private final Clock clock;
    private final ProductMapper productMapper;
    private final FileStorageService fileStorageService;
    private final ProductRepository productRepository;
    private final ProductSpecifications productSpecifications;
    private final FavoriteProductRepository favoriteProductRepository;

    private final Slugify slugify = Slugify.builder().transliterator(true)
            .customReplacement("х", "kh")
            .customReplacement("щ", "shch")
            .customReplacement("ц", "ts")
            .build();

    @Autowired
    public ProductServiceImpl(Clock clock,
                              ProductMapper productMapper,
                              FileStorageService fileStorageService,
                              ProductRepository productRepository,
                              ProductSpecifications productSpecifications,
                              FavoriteProductRepository favoriteProductRepository) {
        this.clock = clock;
        this.productMapper = productMapper;
        this.fileStorageService = fileStorageService;
        this.productRepository = productRepository;
        this.productSpecifications = productSpecifications;
        this.favoriteProductRepository = favoriteProductRepository;
    }


    @Override
    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO productRequestDTO) {

        Product product = productMapper.toEntity(productRequestDTO);

        String productSlug = slugify.slugify(product.getName());

        if (productRepository.existsBySlug(productSlug)) {
            throw new AlreadyExistsException("Product with slug " + productSlug + " already exists");
        }

        product.setSlug(productSlug);

        product.setCreationTime(OffsetDateTime.now(clock));
        product.setRating(BigDecimal.valueOf(0));

        Product savedProduct = productRepository.save(product);

        log.info("product created: {}", savedProduct.getId());

        return getProductMapperFunction().apply(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponseDTO updateProduct(Long productId, ProductRequestDTO productRequestDTO) {

        Product product = findById(productId);

        productMapper.updateEntityFromDto(productRequestDTO, product);

        Product savedProduct = productRepository.save(product);

        return getProductMapperFunction().apply(savedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {

        Product product = findById(productId);

        if (product.getSalesCount() > 0) {

            product.setAvailability(ProductAvailability.ARCHIVED);
            productRepository.save(product);
        } else {

            log.info("Product {} has NO orders. Performing hard delete.", productId);

            List<String> imageKeys = product.getImages().stream()
                    .map(ProductImage::getImageId)
                    .toList();

            productRepository.delete(product);

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
    public ProductResponseDTO getProductBySlug(String slug) {

        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with slug: " + slug));

        return getProductMapperFunction().apply(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(Long productId) {
        return getProductMapperFunction().apply(findById(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getAllProducts(String query, Pageable pageable) {

        if (query == null || query.isBlank()) {
            return productRepository.findAll(pageable)
                    .map(getProductMapperFunction());
        }

        ProductSearchRequestDTO searchRequest = new ProductSearchRequestDTO(
                query, null, null, null, null, null
        );

        Specification<Product> spec = productSpecifications.getSpecifications(searchRequest, false);

        return productRepository.findAll(spec, pageable)
                .map(getProductMapperFunction());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getAllProductByCategory(ProductCategory productCategory, int page, int size) {
        return productRepository.findAllByProductCategory(productCategory, PageRequest.of(page, size)).map(getProductMapperFunction());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getNewProducts(int page, int size) {

        return productRepository.findAll(PageRequest.of(page, size, Sort.by("creationTime").descending()))
                .map(getProductMapperFunction());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getProductsWithDiscount(int page, int size) {
        return productRepository.findAllWithDiscount(PageRequest.of(page, size, Sort.by("creationTime")))
                .map(getProductMapperFunction());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getBestsellers(int page, int size) {
        return productRepository.findAll(PageRequest.of(page, size, Sort.by("salesCount").descending()))
                .map(getProductMapperFunction());
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
                products.map(getProductMapperFunction()),
                subcategories,
                minMax.get("minPrice"),
                minMax.get("maxPrice")
        );
    }

    private Function<Product, ProductResponseDTO> getProductMapperFunction() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();


        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return productMapper::toResponseDTO;
        }

        try {
            UUID userId = UUID.fromString(authentication.getName());

            Set<Long> favoriteIds = favoriteProductRepository.findProductIdsByUserPublicId(userId);

            return product -> {
                boolean isFav = favoriteIds.contains(product.getId());
                return productMapper.toResponseDTO(product, isFav);
            };

        } catch (IllegalArgumentException e) {
            log.error("User ID is not a valid UUID: {}", authentication.getName());
            return productMapper::toResponseDTO;
        }
    }
}
