package ua.moki.modules.products.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.products.domains.FavoriteProduct;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.dtos.ProductResponseDTO;
import ua.moki.modules.products.repositories.FavoriteProductRepository;
import ua.moki.modules.products.services.FavoriteProductService;
import ua.moki.modules.products.services.ProductService;
import ua.moki.modules.products.utils.mappers.ProductMapper;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.repositories.UserRepository;
import ua.moki.modules.users.services.UserService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FavoriteProductServiceImpl implements FavoriteProductService {

    private final ProductMapper productMapper;
    private final UserService userService;
    private final ProductService productService;
    private final FavoriteProductRepository favoriteProductRepository;

    @Autowired
    public FavoriteProductServiceImpl(UserService userService,
                                      ProductService productService,
                                      FavoriteProductRepository favoriteProductRepository,
                                      ProductMapper productMapper) {
        this.userService = userService;
        this.productService = productService;
        this.favoriteProductRepository = favoriteProductRepository;
        this.productMapper = productMapper;
    }


    @Override
    @Transactional
    public void createFavoriteProduct(UUID userId, long productId) {

        if (favoriteProductRepository.existsByUser_PublicIdAndProductId(userId, productId)) return;

        User user = userService.getActiveUserEntityByPublicId(userId);
        Product product = productService.findById(productId);

        FavoriteProduct favoriteProduct = new FavoriteProduct(user, product);
        favoriteProductRepository.save(favoriteProduct);
    }

    @Override
    @Transactional
    public void deleteFavoriteProduct(UUID userId, long productId) {
        Optional<FavoriteProduct> optional = favoriteProductRepository.findByUser_PublicIdAndProductId(userId, productId);

        if (optional.isEmpty()) return;

        favoriteProductRepository.delete(optional.get());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getFavoriteProductsByUserId(UUID userId, Pageable pageable) {

        Sort prioritySort = Sort.by(Sort.Order.desc("product.availability"))
                .and(pageable.getSort());

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                prioritySort
        );

        return favoriteProductRepository.findByUser_PublicId(userId, sortedPageable)
                .map(FavoriteProduct::getProduct)
                .map(product -> productMapper.toResponseDTO(product, true));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getListOfFavoriteProductId(UUID userId) {
        return favoriteProductRepository.findProductIdsByUserPublicId(userId);
    }
}
