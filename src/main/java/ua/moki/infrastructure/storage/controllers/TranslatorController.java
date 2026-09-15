package ua.moki.infrastructure.storage.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.moki.infrastructure.storage.dtos.TranslatorDTO;
import ua.moki.infrastructure.storage.service.TranslatorService;

@RestController
@RequestMapping("/translator")
@RequiredArgsConstructor
public class TranslatorController {

    private final TranslatorService translatorService;

    @PostMapping(value = "/to-ru")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TranslatorDTO> translateInfoToRu(@RequestBody TranslatorDTO translateUaDTO) {

        TranslatorDTO translatingContent = translatorService.translateTextsToRu(translateUaDTO);

        System.out.println(translatingContent.characteristics().toString());
        return ResponseEntity.ok(translatingContent);
    }
}
