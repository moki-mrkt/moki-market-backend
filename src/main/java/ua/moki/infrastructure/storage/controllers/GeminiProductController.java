package ua.moki.infrastructure.storage.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ua.moki.infrastructure.storage.dtos.DescriptionGeneratorDTO;
import ua.moki.infrastructure.storage.service.GeminiProductGenerator;

@RestController
@RequestMapping("/generator")
@RequiredArgsConstructor
public class GeminiProductController {

    private final GeminiProductGenerator geminiProductGenerator;

    @PostMapping(value = "/description")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @SecurityRequirements()
    public ResponseEntity<String> generateDescription(@RequestBody DescriptionGeneratorDTO descriptionGeneratorDTO) {

        String generateDescription = geminiProductGenerator.generateDescription(descriptionGeneratorDTO.nameProduct(), descriptionGeneratorDTO.attributes());

        return ResponseEntity.ok(generateDescription);
    }

}
