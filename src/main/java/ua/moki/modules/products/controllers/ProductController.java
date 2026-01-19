package ua.moki.modules.products.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ua.moki.modules.products.dtos.ProductRequestDTO;
import ua.moki.modules.products.dtos.ProductResponseDTO;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.services.ProductService;

import java.net.URI;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProductResponseDTO> createProduct(@RequestBody @Valid ProductRequestDTO productRequestDTO) {

        ProductResponseDTO productResponseDTO = productService.createProduct(productRequestDTO);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(productResponseDTO.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(productResponseDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProductResponseDTO> updateProduct(@PathVariable Long id,
                                                            @Valid @RequestBody ProductRequestDTO productRequestDTO) {

        ProductResponseDTO updatedProduct = productService.updateProduct(id, productRequestDTO);

        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {

        productService.deleteProduct(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @SecurityRequirements()
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable Long id) {

        ProductResponseDTO product =  productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<ProductResponseDTO>> getAllProducts(@RequestParam @Min(0) int page, @RequestParam @Min(0) int size) {
        Page<ProductResponseDTO> products = productService.getAllProducts(page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/new")
    @SecurityRequirements()
    public ResponseEntity<Page<ProductResponseDTO>> getNewProducts(@RequestParam @Min(0) int page, @RequestParam @Min(0) int size) {
        Page<ProductResponseDTO> products = productService.getNewProducts(page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/category/{category}")
    @SecurityRequirements()
    public ResponseEntity<Page<ProductResponseDTO>> getAllProductsByCategory(@PathVariable ProductCategory category,
                                                                          @RequestParam @Min(0) int page,
                                                                          @RequestParam @Min(0) int size) {
        Page<ProductResponseDTO> products = productService.getAllProductByCategory(category, page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/discount")
    @SecurityRequirements()
    public ResponseEntity<Page<ProductResponseDTO>> getProductsWithDiscount(@RequestParam @Min(0) int page, @RequestParam @Min(0) int size) {
        Page<ProductResponseDTO> products = productService.getProductsWithDiscount(page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/bestsellers")
    @SecurityRequirements()
    public ResponseEntity<Page<ProductResponseDTO>> getBestsellers(@RequestParam @Min(0) int page, @RequestParam @Min(0) int size) {
        Page<ProductResponseDTO> products = productService.getBestsellers(page, size);
        return ResponseEntity.ok(products);
    }


}
