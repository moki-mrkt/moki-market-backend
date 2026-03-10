package ua.moki.modules.products.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.moki.modules.products.dtos.ProductResponseDTO;
import ua.moki.modules.products.services.FavoriteProductService;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/favorites")
public class FavoriteProductController {

    private final FavoriteProductService favoriteProductService;

    @Autowired
    public FavoriteProductController(FavoriteProductService favoriteProductService) {
        this.favoriteProductService = favoriteProductService;
    }

    @PostMapping("/{productId}")
    public ResponseEntity<Void> createFavoriteProduct(Principal principal, @PathVariable long productId) {

        favoriteProductService.createFavoriteProduct(UUID.fromString(principal.getName()), productId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteFavoriteProduct(Principal principal, @PathVariable long productId) {

        favoriteProductService.deleteFavoriteProduct(UUID.fromString(principal.getName()), productId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user")
    public ResponseEntity<Page<ProductResponseDTO>> getFavoriteProducts(Principal principal,
                                                                  @RequestParam int page,
                                                                  @RequestParam int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<ProductResponseDTO> result = favoriteProductService.getFavoriteProductsByUserId(UUID.fromString(principal.getName()), pageable);

        return ResponseEntity.ok(result);

    }

    @GetMapping("/user/list")
    public ResponseEntity<Set<Long>> getFavoriteProducts(Principal principal) {

        Set<Long> result = favoriteProductService.getListOfFavoriteProductId(UUID.fromString(principal.getName()));

        return ResponseEntity.ok(result);
    }
}
