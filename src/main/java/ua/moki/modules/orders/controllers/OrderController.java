package ua.moki.modules.orders.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ua.moki.modules.orders.dtos.OrderRequestDTO;
import ua.moki.modules.orders.dtos.OrderResponseDTO;
import ua.moki.modules.orders.dtos.OrderUpdateDTO;
import ua.moki.modules.orders.services.OrderService;

import java.net.URI;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("permitAll()")
    @SecurityRequirements()
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody @Valid OrderRequestDTO dto) {

        OrderResponseDTO orderResponseDTO = orderService.createOrder(dto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(orderResponseDTO.id())
                .toUri();

        return ResponseEntity.created(location).body(orderResponseDTO);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponseDTO> updateOrder(@PathVariable UUID id,
                                                        @RequestBody @Valid OrderUpdateDTO dto) {

        OrderResponseDTO orderResponseDTO = orderService.updateOrder(id, dto);

        return ResponseEntity.ok(orderResponseDTO);
    }

    @DeleteMapping("/cancel/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID id) {

        orderService.cancelOrder(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable UUID id) {
        OrderResponseDTO order = orderService.getOrderByPublicId(id);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<OrderResponseDTO>> getOrdersByUser(Principal principal,
                                                           @RequestParam @Min(0) int page,
                                                           @RequestParam @Min(0) int size) {
        UUID userId = UUID.fromString(principal.getName());
        Page<OrderResponseDTO> result = orderService.getOrdersByUserId(userId, page, size);

        return ResponseEntity.ok(result);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<OrderResponseDTO>> getAllOrders(@RequestParam @Min(0) int page,
                                                               @RequestParam @Min(0) int size) {
        Page<OrderResponseDTO> result = orderService.getAllOrders(page, size);
        return ResponseEntity.ok(result);
    }




}
