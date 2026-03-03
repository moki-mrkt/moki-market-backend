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
@RequestMapping("/carts")
public class CartController {

    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CartResponseDTO> addToCart(Principal principal,
                                                     @RequestParam Long productId,
                                                     @RequestParam @Min(1) int quantity) {
        UUID userId = UUID.fromString(principal.getName());
        CartResponseDTO cartResponseDTO = cartService.addToCart(userId, productId, quantity);
        return ResponseEntity.ok(cartResponseDTO);
    }

    @DeleteMapping("/clear")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Void> clearCart(Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

}
