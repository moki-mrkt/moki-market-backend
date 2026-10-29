package ua.moki.modules.products.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ua.moki.modules.products.dtos.ProductImportResultDTO;
import ua.moki.modules.products.services.ProductImportService;

import java.io.IOException;

@RestController
@RequestMapping("/import")
@RequiredArgsConstructor
public class ProductImportController {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ProductImportService productImportService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProductImportResultDTO> importProducts(@RequestParam("file") MultipartFile file) {
        ProductImportResultDTO result = productImportService.importProductsFromExcel(file);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        byte[] templateBytes = productImportService.generateTemplate();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"moki-products-import-template.xlsx\"")
                .contentType(MediaType.parseMediaType(XLSX_CONTENT_TYPE))
                .body(templateBytes);
    }
}
