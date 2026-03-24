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
import ua.moki.modules.orders.repositories.CartItemRepository;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.dtos.*;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private final Clock clock;
    private final ProductMapper productMapper;
    private final CartItemRepository cartItemRepository;
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
                              CartItemRepository cartItemRepository,
                              ProductRepository productRepository,
                              ProductSpecifications productSpecifications,
                              FavoriteProductRepository favoriteProductRepository) {
        this.clock = clock;
        this.productMapper = productMapper;
        this.cartItemRepository = cartItemRepository;
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

        String newProductSlug = slugify.slugify(product.getName());
        product.setSlug(newProductSlug);

        Product savedProduct = productRepository.save(product);

        return getProductMapperFunction().apply(savedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {

        Product product = findById(productId);

        product.setAvailability(ProductAvailability.ARCHIVED);
        productRepository.save(product);

        cartItemRepository.deleteByProduct(product);
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
    public ProductAdminResponseDTO getProductByIdForAdmin(Long productId) {
        return productMapper.toAdminResponseDTO(findById(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductPublicDTO> getAllProductsForPublic(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(productMapper::toPublicDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductAdminResponseDTO> getAllProducts(String query, Pageable pageable) {

        if (query == null || query.isBlank()) {
            return productRepository.findAll(pageable)
                    .map(productMapper::toAdminResponseDTO);
        }

        ProductSearchRequestDTO searchRequest = new ProductSearchRequestDTO(
                query, null, null, null, null, null
        );

        Specification<Product> spec = productSpecifications.getSpecifications(searchRequest, false);

        return productRepository.findAll(spec, pageable)
                .map(productMapper::toAdminResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getAllProductByCategory(ProductCategory productCategory, int page, int size) {
        return productRepository.findAllByProductCategoryAndAvailability(productCategory, ProductAvailability.IN_STOCK, PageRequest.of(page, size)).map(getProductMapperFunction());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getNewProducts(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("creationTime").descending());

        return productRepository.findAllByAvailability(pageable, ProductAvailability.IN_STOCK)
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

        Pageable pageable = PageRequest.of(page, size, Sort.by("salesCount").descending());

        return productRepository.findAllByAvailability(pageable, ProductAvailability.IN_STOCK)
                .map(getProductMapperFunction());
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResponse searchProducts(ProductSearchRequestDTO request, Pageable pageable) {

        Specification<Product> productSpec = productSpecifications.getSpecifications(request, false);

        productSpec = productSpec.and((root, query, cb) ->
                cb.notEqual(root.get("availability"), ProductAvailability.ARCHIVED)
        );

        Sort prioritySort = Sort.by(Sort.Order.asc("availability"))
                .and(pageable.getSort());

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                prioritySort
        );

        Page<Product> products = productRepository.findAll(productSpec, sortedPageable);

        Specification<Product> facetSpec = productSpecifications.getSpecifications(request, true)
                .and((root, query, cb) -> cb.notEqual(root.get("availability"), ProductAvailability.ARCHIVED));

        List<String> subcategories = productRepository.findDistinctSubcategoriesBySpec(facetSpec);

        ProductSearchRequestDTO limitRequest = new ProductSearchRequestDTO(
                request.query(), request.category(), null, null,
                request.subcategory(), request.hasDiscount()
        );

        Specification<Product> limitSpec = productSpecifications.getSpecifications(limitRequest, false)
                .and((root, query, cb) -> cb.notEqual(root.get("availability"), ProductAvailability.ARCHIVED));

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
