package ua.moki.modules.products.services.impl;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.domains.ProductImage;
import ua.moki.modules.products.dtos.ProductRequestDTO;
import ua.moki.modules.products.dtos.ProductResponseDTO;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.repositories.ProductRepository;
import ua.moki.modules.products.services.ProductService;
import ua.moki.modules.products.utils.mappers.ProductMapper;
import ua.moki.util.exceptions.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private final Clock clock;
    private final ProductMapper productMapper;
    private final ProductRepository productRepository;

    @Autowired
    public ProductServiceImpl(Clock clock, ProductMapper productMapper, ProductRepository productRepository) {
        this.clock = clock;
        this.productMapper = productMapper;
        this.productRepository = productRepository;
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

        log.info("product created: {}", savedProduct);

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

        boolean hasOrders = true; //   orderItemRepository.existsByProductId(id);

        if (hasOrders) {

            product.setAvailability(ProductAvailability.ARCHIVED);
            productRepository.save(product);

            // Прибираємо з вітрини (Elastic)
            // productSearchRepository.deleteById(id);
        } else {

            log.info("Product {} has NO orders. Performing hard delete.", productId);

            List<String> imageKeys = product.getImages().stream()
                    .map(ProductImage::getImageId)
                    .toList();

            productRepository.delete(product);

            // 3. Чистимо S3 (асинхронно або тут же, якщо ключів мало)
          //  if (!imageKeys.isEmpty()) fileStorageService.deleteFiles(imageKeys);
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
    public Page<ProductResponseDTO> getAllProducts(int page, int size) {
        return productRepository.findAll(PageRequest.of(page, size)).map(productMapper::toResponseDTO);
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
}
