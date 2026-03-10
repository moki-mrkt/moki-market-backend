package ua.moki.modules.products.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import ua.moki.modules.products.dtos.ProductResponseDTO;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface FavoriteProductService {

    void createFavoriteProduct(UUID userId, long productId);
    void deleteFavoriteProduct(UUID userId, long productId);
    Page<ProductResponseDTO> getFavoriteProductsByUserId(UUID userId, Pageable pageable);
    Set<Long> getListOfFavoriteProductId(UUID userId);
}
