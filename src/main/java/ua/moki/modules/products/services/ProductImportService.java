package ua.moki.modules.products.services;

import org.springframework.web.multipart.MultipartFile;
import ua.moki.modules.products.dtos.ProductImportResultDTO;

import java.io.IOException;

public interface ProductImportService {

    ProductImportResultDTO importProductsFromExcel(MultipartFile file);

    byte[] generateTemplate() throws IOException;
}
