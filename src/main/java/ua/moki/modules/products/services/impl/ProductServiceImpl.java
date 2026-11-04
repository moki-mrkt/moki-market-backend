package ua.moki.modules.products.services.impl;


import com.github.slugify.Slugify;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ua.moki.infrastructure.storage.dtos.GeneratedKeywordsDTO;
import ua.moki.infrastructure.storage.dtos.KeywordsGeneratorRequestDTO;
import ua.moki.infrastructure.storage.service.GeminiProductGenerator;
import ua.moki.modules.analytics.events.SearchPerformedEvent;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final ApplicationEventPublisher eventPublisher;
    private final GeminiProductGenerator geminiProductGenerator;
    private final Executor taskExecutor;

    private final AtomicBoolean isGeneratingAllKeywords = new AtomicBoolean(false);
    private final AtomicInteger bgTotalProducts = new AtomicInteger(0);
    private final AtomicInteger bgProcessedProducts = new AtomicInteger(0);
    private final AtomicInteger bgUpdatedProducts = new AtomicInteger(0);
    private final AtomicInteger bgSkippedProducts = new AtomicInteger(0);
    private final AtomicInteger bgFailedProducts = new AtomicInteger(0);
    private volatile String bgCurrentProductName = null;
    private volatile String bgMessage = "Немає активних фонових задач";

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
                              FavoriteProductRepository favoriteProductRepository,
                              ApplicationEventPublisher eventPublisher,
                              GeminiProductGenerator geminiProductGenerator,
                              @Qualifier("taskExecutor") Executor taskExecutor) {
        this.clock = clock;
        this.productMapper = productMapper;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.productSpecifications = productSpecifications;
        this.favoriteProductRepository = favoriteProductRepository;
        this.eventPublisher = eventPublisher;
        this.geminiProductGenerator = geminiProductGenerator;
        this.taskExecutor = taskExecutor;
    }


    @Override
    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO productRequestDTO) {

        Product product = productMapper.toEntity(productRequestDTO);

        String productSlug = slugify.slugify(product.getName().toLowerCase());

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

        String newProductSlug = slugify.slugify(product.getName().toLowerCase());
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

        Sort prioritySort = Sort.by(
                Sort.Order.asc("availability"),
                Sort.Order.asc("groupId").nullsLast()
        ).and(pageable.getSort());

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

        if (pageable.getPageNumber() == 0) {
            publishSearchAnalyticsEvent(request, products.getTotalElements());
        }

        return new SearchResponse(
                products.map(getProductMapperFunction()),
                subcategories,
                minMax.get("minPrice"),
                minMax.get("maxPrice")
        );
    }

    @Override
    public BulkKeywordsGenerationResultDTO generateKeywordsForAllProducts(boolean overwrite) {
        if (!isGeneratingAllKeywords.compareAndSet(false, true)) {
            log.warn("Bulk keywords generation is already in progress. Ignoring duplicate request.");
            throw new IllegalStateException("Процес генерації пошукових запитів для всіх товарів вже виконується.");
        }

        try {
            List<Long> productIds = productRepository.findAllProductIds();
            int total = productIds.size();
            int updated = 0;
            int skipped = 0;
            int failed = 0;

            for (Long productId : productIds) {
                Product product = productRepository.findById(productId).orElse(null);
                if (product == null) {
                    continue;
                }

                boolean hasKeywordsUa = product.getKeywords() != null && !product.getKeywords().isEmpty();
                boolean hasKeywordsRu = product.getKeywordsRu() != null && !product.getKeywordsRu().isEmpty();

                if (!overwrite && hasKeywordsUa && hasKeywordsRu) {
                    skipped++;
                    continue;
                }

                try {
                    String categoryTitle = product.getProductCategory() != null
                            ? product.getProductCategory().getTitle()
                            : "";

                    GeneratedKeywordsDTO generated = geminiProductGenerator.generateKeywords(
                            new KeywordsGeneratorRequestDTO(
                                    product.getName(),
                                    categoryTitle,
                                    product.getSubcategory(),
                                    product.getManufacturerOfTheProduct()
                            )
                    );

                    if (generated != null && generated.keywords() != null && !generated.keywords().isEmpty()) {
                        List<String> sanitizedUa = productMapper.sanitizeKeywords(generated.keywords());
                        List<String> ruList = (generated.keywordsRu() != null && !generated.keywordsRu().isEmpty())
                                ? generated.keywordsRu()
                                : generated.keywords();
                        List<String> sanitizedRu = productMapper.sanitizeKeywords(ruList);

                        boolean saved = false;
                        for (int attempt = 0; attempt < 2 && !saved; attempt++) {
                            try {
                                Product fresh = productRepository.findById(productId).orElse(null);
                                if (fresh == null) break;
                                fresh.setKeywords(sanitizedUa);
                                fresh.setKeywordsRu(sanitizedRu);
                                productRepository.saveAndFlush(fresh);
                                saved = true;
                                updated++;
                                log.info("Generated keywords for product id={} ('{}')", fresh.getId(), fresh.getName());
                            } catch (Exception lockEx) {
                                if (attempt == 1) {
                                    throw lockEx;
                                }
                                log.warn("Optimistic lock conflict for product id={}, retrying with fresh state...", productId);
                            }
                        }
                    } else {
                        failed++;
                    }
                } catch (Exception e) {
                    failed++;
                    log.error("Failed to generate keywords for product id={}: {}", productId, e.getMessage());
                }
            }

            return new BulkKeywordsGenerationResultDTO(total, updated, skipped, failed);
        } finally {
            isGeneratingAllKeywords.set(false);
        }
    }

    @Override
    public BulkKeywordsGenerationStatusDTO getKeywordsGenerationStatus() {
        return new BulkKeywordsGenerationStatusDTO(
                isGeneratingAllKeywords.get(),
                bgTotalProducts.get(),
                bgProcessedProducts.get(),
                bgUpdatedProducts.get(),
                bgSkippedProducts.get(),
                bgFailedProducts.get(),
                bgCurrentProductName,
                bgMessage
        );
    }

    @Override
    public BulkKeywordsGenerationStatusDTO startBackgroundKeywordsGeneration(boolean overwrite) {
        if (!isGeneratingAllKeywords.compareAndSet(false, true)) {
            return getKeywordsGenerationStatus();
        }

        List<Long> productIds = productRepository.findAllProductIds();
        bgTotalProducts.set(productIds.size());
        bgProcessedProducts.set(0);
        bgUpdatedProducts.set(0);
        bgSkippedProducts.set(0);
        bgFailedProducts.set(0);
        bgCurrentProductName = null;
        bgMessage = "Генерацію пошукових запитів розпочато у фоновому режимі";

        CompletableFuture.runAsync(() -> {
            try {
                for (Long productId : productIds) {
                    Product product = productRepository.findById(productId).orElse(null);
                    if (product == null) {
                        bgProcessedProducts.incrementAndGet();
                        continue;
                    }

                    bgCurrentProductName = product.getName();

                    boolean hasKeywordsUa = product.getKeywords() != null && !product.getKeywords().isEmpty();
                    boolean hasKeywordsRu = product.getKeywordsRu() != null && !product.getKeywordsRu().isEmpty();

                    if (!overwrite && hasKeywordsUa && hasKeywordsRu) {
                        bgSkippedProducts.incrementAndGet();
                        bgProcessedProducts.incrementAndGet();
                        continue;
                    }

                    try {
                        String categoryTitle = product.getProductCategory() != null
                                ? product.getProductCategory().getTitle()
                                : "";

                        GeneratedKeywordsDTO generated = geminiProductGenerator.generateKeywords(
                                new KeywordsGeneratorRequestDTO(
                                        product.getName(),
                                        categoryTitle,
                                        product.getSubcategory(),
                                        product.getManufacturerOfTheProduct()
                                )
                        );

                        if (generated != null && generated.keywords() != null && !generated.keywords().isEmpty()) {
                            List<String> sanitizedUa = productMapper.sanitizeKeywords(generated.keywords());
                            List<String> ruList = (generated.keywordsRu() != null && !generated.keywordsRu().isEmpty())
                                    ? generated.keywordsRu()
                                    : generated.keywords();
                            List<String> sanitizedRu = productMapper.sanitizeKeywords(ruList);

                            boolean saved = false;
                            for (int attempt = 0; attempt < 2 && !saved; attempt++) {
                                try {
                                    Product fresh = productRepository.findById(productId).orElse(null);
                                    if (fresh == null) break;
                                    fresh.setKeywords(sanitizedUa);
                                    fresh.setKeywordsRu(sanitizedRu);
                                    productRepository.saveAndFlush(fresh);
                                    saved = true;
                                    bgUpdatedProducts.incrementAndGet();
                                    log.info("[Background] Generated keywords for product id={} ('{}')", fresh.getId(), fresh.getName());
                                } catch (Exception lockEx) {
                                    if (attempt == 1) throw lockEx;
                                    log.warn("Optimistic lock conflict for product id={}, retrying...", productId);
                                }
                            }
                        } else {
                            bgFailedProducts.incrementAndGet();
                        }
                    } catch (Exception e) {
                        bgFailedProducts.incrementAndGet();
                        log.error("[Background] Failed to generate keywords for product id={}: {}", productId, e.getMessage());
                    } finally {
                        bgProcessedProducts.incrementAndGet();
                    }
                }
                bgMessage = String.format("Фонову генерацію завершено: оновлено %d, пропущено %d, помилок %d",
                        bgUpdatedProducts.get(), bgSkippedProducts.get(), bgFailedProducts.get());
                log.info("[Background] Bulk keywords generation completed. {}", bgMessage);
            } catch (Exception e) {
                bgMessage = "Помилка фонової генерації: " + e.getMessage();
                log.error("[Background] Unexpected error in bulk keywords generation", e);
            } finally {
                bgCurrentProductName = null;
                isGeneratingAllKeywords.set(false);
            }
        }, taskExecutor);

        return getKeywordsGenerationStatus();
    }

    private void publishSearchAnalyticsEvent(ProductSearchRequestDTO request, long totalElements) {
        try {
            UUID userPublicId = extractCurrentUserPublicId();
            String locale = LocaleContextHolder.getLocale().getLanguage();
            String clientIp = null;
            String userAgent = null;

            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                HttpServletRequest httpRequest = attrs.getRequest();
                clientIp = extractClientIp(httpRequest);
                userAgent = httpRequest.getHeader("User-Agent");
            }

            eventPublisher.publishEvent(new SearchPerformedEvent(
                    request.query(),
                    totalElements,
                    request.category(),
                    request.subcategory(),
                    request.minPrice() != null ? BigDecimal.valueOf(request.minPrice()) : null,
                    request.maxPrice() != null ? BigDecimal.valueOf(request.maxPrice()) : null,
                    request.hasDiscount(),
                    userPublicId,
                    locale,
                    clientIp,
                    userAgent,
                    OffsetDateTime.now(clock)
            ));
        } catch (Exception e) {
            log.warn("Failed to publish search analytics event: {}", e.getMessage());
        }
    }

    private UUID extractCurrentUserPublicId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            try {
                return UUID.fromString(authentication.getName());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private Function<Product, ProductResponseDTO> getProductMapperFunction() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return product -> {
            boolean isFav = false;

            if (authentication != null &&
                    authentication.isAuthenticated() &&
                    !(authentication instanceof AnonymousAuthenticationToken)) {
                try {
                    UUID userId = UUID.fromString(authentication.getName());
                    Set<Long> favoriteIds = favoriteProductRepository.findProductIdsByUserPublicId(userId);
                    isFav = favoriteIds.contains(product.getId());
                } catch (IllegalArgumentException e) {
                    log.error("User ID is not a valid UUID: {}", authentication.getName());
                }
            }

            List<ProductVariantDTO> variants = null;
            if (product.getProductType() == ua.moki.modules.products.enums.ProductType.VARIANT
                    && product.getGroupId() != null
                    && !product.getGroupId().isBlank()) {

                variants = productRepository.findByGroupId(product.getGroupId()).stream()
                        .map(sibling -> new ProductVariantDTO(
                                sibling.getId(),
                                sibling.getSlug(),
                                sibling.getVariantName(),
                                sibling.getVariantValue(),
                                sibling.getId().equals(product.getId())
                        ))
                        .toList();
            }

            return productMapper.toResponseDTOWithVariants(product, isFav, variants);
        };
    }
}
