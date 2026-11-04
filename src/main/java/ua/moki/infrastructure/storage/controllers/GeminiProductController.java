package ua.moki.infrastructure.storage.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ua.moki.infrastructure.storage.dtos.CharacteristicsGeneratorRequestDTO;
import ua.moki.infrastructure.storage.dtos.DescriptionGeneratorDTO;
import ua.moki.infrastructure.storage.dtos.GeneratedCharacteristicsDTO;
import ua.moki.infrastructure.storage.dtos.GeneratedKeywordsDTO;
import ua.moki.infrastructure.storage.dtos.KeywordsGeneratorRequestDTO;
import ua.moki.infrastructure.storage.service.GeminiProductGenerator;
import ua.moki.modules.products.dtos.BulkKeywordsGenerationResultDTO;
import ua.moki.modules.products.dtos.BulkKeywordsGenerationStatusDTO;
import ua.moki.modules.products.services.ProductService;

@RestController
@RequestMapping("/generator")
@RequiredArgsConstructor
public class GeminiProductController {

    private final GeminiProductGenerator geminiProductGenerator;
    private final ProductService productService;

    @PostMapping(value = "/description")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @SecurityRequirements()
    public ResponseEntity<String> generateDescription(@RequestBody DescriptionGeneratorDTO descriptionGeneratorDTO) {

        String generateDescription = geminiProductGenerator.generateDescription(
                descriptionGeneratorDTO.nameProduct(),
                descriptionGeneratorDTO.attributes()
        );

        return ResponseEntity.ok(generateDescription);
    }

    @PostMapping(value = "/characteristics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @SecurityRequirements()
    public ResponseEntity<GeneratedCharacteristicsDTO> generateCharacteristics(
            @RequestBody CharacteristicsGeneratorRequestDTO requestDTO
    ) {
        GeneratedCharacteristicsDTO generated = geminiProductGenerator.generateCharacteristics(requestDTO);
        return ResponseEntity.ok(generated);
    }

    @PostMapping(value = "/keywords")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @SecurityRequirements()
    public ResponseEntity<GeneratedKeywordsDTO> generateKeywords(
            @RequestBody KeywordsGeneratorRequestDTO requestDTO
    ) {
        GeneratedKeywordsDTO generated = geminiProductGenerator.generateKeywords(requestDTO);
        return ResponseEntity.ok(generated);
    }

    @PostMapping(value = "/keywords/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @SecurityRequirements()
    public ResponseEntity<BulkKeywordsGenerationStatusDTO> generateKeywordsForAllProducts(
            @RequestParam(defaultValue = "false") boolean overwrite
    ) {
        BulkKeywordsGenerationStatusDTO status = productService.startBackgroundKeywordsGeneration(overwrite);
        return ResponseEntity.ok(status);
    }

    @GetMapping(value = "/keywords/all/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @SecurityRequirements()
    public ResponseEntity<BulkKeywordsGenerationStatusDTO> getKeywordsGenerationStatus() {
        return ResponseEntity.ok(productService.getKeywordsGenerationStatus());
    }

}
