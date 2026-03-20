package ua.moki.modules.products.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.dtos.*;
import ua.moki.modules.products.enums.ProductCategory;


public interface ProductService {

    ProductResponseDTO createProduct(ProductRequestDTO productRequestDTO);
    ProductResponseDTO updateProduct(Long productId, ProductRequestDTO productRequestDTO);
    void deleteProduct(Long productId);
    Product findById(Long productId);
    ProductResponseDTO getProductBySlug(String slug);
    ProductResponseDTO getProductById(Long productId);
    ProductAdminResponseDTO getProductByIdForAdmin(Long productId);

    Page<ProductPublicDTO> getAllProductsForPublic(Pageable pageable);
    Page<ProductAdminResponseDTO> getAllProducts(String query, Pageable pageable);
    Page<ProductResponseDTO> getAllProductByCategory(ProductCategory productCategory, int page, int size);

    Page<ProductResponseDTO> getNewProducts(int page, int size);
    Page<ProductResponseDTO> getProductsWithDiscount(int page, int size);
    Page<ProductResponseDTO> getBestsellers(int page, int size);

    SearchResponse searchProducts(ProductSearchRequestDTO request, Pageable pageable);


}
