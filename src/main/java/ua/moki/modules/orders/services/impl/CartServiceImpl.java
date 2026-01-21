package ua.moki.modules.orders.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.orders.domains.Cart;
import ua.moki.modules.orders.domains.CartItem;
import ua.moki.modules.orders.dtos.CartResponseDTO;
import ua.moki.modules.orders.repositories.CartRepository;
import ua.moki.modules.orders.services.CartService;
import ua.moki.modules.orders.utils.mappers.CartMapper;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.repositories.ProductRepository;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.repositories.UserRepository;
import ua.moki.util.exceptions.EntityNotFoundException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

    @Override
    public CartResponseDTO addToCart(UUID userId, Long productId, int quantity) {

        Cart cart = cartRepository.findCartByUser_PublicId(userId)
                .orElseGet(() -> createCartForUser(userId));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        existingItem.ifPresentOrElse(
                item -> item.setQuantity(item.getQuantity() + quantity),
                () -> createAndAddNewItem(cart, productId, quantity)
        );

        cart.setUpdatedAt(OffsetDateTime.now());
        cartRepository.save(cart);

        return cartMapper.toDto(cart);
    }

    private void createAndAddNewItem(Cart cart, Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        CartItem newItem = new CartItem();
        newItem.setProduct(product);
        newItem.setQuantity(quantity);

        cart.addItem(newItem);
    }

    private Cart createCartForUser(UUID userPublicId) {
        User user = userRepository.findByPublicId(userPublicId).orElseThrow();
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setUpdatedAt(OffsetDateTime.now());
        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        cartRepository.findCartByUser_PublicId(userId)
                .ifPresent(cart -> {
                    cart.getItems().clear();
                    cartRepository.save(cart);
                });
    }
}
