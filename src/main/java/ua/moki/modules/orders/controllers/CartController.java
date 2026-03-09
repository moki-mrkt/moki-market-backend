package ua.moki.modules.orders.controllers;

import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ua.moki.modules.orders.dtos.CartResponseDTO;
import ua.moki.modules.orders.services.CartService;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/cart")
@PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
public class CartController {

    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponseDTO> addToCart(Principal principal,
                                                     @RequestParam Long productId,
                                                     @RequestParam @Min(1) int quantity) {
        UUID userId = UUID.fromString(principal.getName());
        CartResponseDTO cartResponseDTO = cartService.addToCart(userId, productId, quantity);
        return ResponseEntity.ok(cartResponseDTO);
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponseDTO> updateQuantity(Principal principal,
                                                          @PathVariable Long productId,
                                                          @RequestParam @Min(1) int quantity) {
        UUID userId = UUID.fromString(principal.getName());
        CartResponseDTO cartResponseDTO = cartService.updateItemQuantity(userId, productId, quantity);
        return ResponseEntity.ok(cartResponseDTO);
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponseDTO> deleteItemFromCart(Principal principal,
                                                              @PathVariable Long productId) {
        UUID userId = UUID.fromString(principal.getName());
        CartResponseDTO cartResponseDTO = cartService.deleteItemFromCart(userId, productId);
        return ResponseEntity.ok(cartResponseDTO);
    }

    @GetMapping
    public ResponseEntity<CartResponseDTO> getCart(Principal principal) {

        UUID userId = UUID.fromString(principal.getName());
        CartResponseDTO cartResponseDTO = cartService.getCart(userId);

        return ResponseEntity.ok(cartResponseDTO);
    }

}
