package ua.moki.modules.products.services;

import org.springframework.data.domain.Page;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.dtos.ProductRequestDTO;
import ua.moki.modules.products.dtos.ProductResponseDTO;
import ua.moki.modules.products.enums.ProductCategory;


public interface ProductService {

    ProductResponseDTO createProduct(ProductRequestDTO productRequestDTO);
    ProductResponseDTO updateProduct(Long productId, ProductRequestDTO productRequestDTO);
    void deleteProduct(Long productId);
    Product findById(Long productId);
    ProductResponseDTO getProductById(Long productId);

    Page<ProductResponseDTO> getAllProducts(int page, int size);
    Page<ProductResponseDTO> getAllProductByCategory(ProductCategory productCategory, int page, int size);

    Page<ProductResponseDTO> getNewProducts(int page, int size);
    Page<ProductResponseDTO> getProductsWithDiscount(int page, int size);
    Page<ProductResponseDTO> getBestsellers(int page, int size);


}
