package ua.moki.modules.products.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.moki.modules.products.services.YmlExportService;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportProductController {

    private final YmlExportService ymlExportService;

    @GetMapping(value = "/rozetka/candies", produces = MediaType.APPLICATION_XML_VALUE)
    @PreAuthorize("permitAll()")
    @SecurityRequirements()
    public ResponseEntity<String> rozetkaCandies() {
        return ResponseEntity.ok(ymlExportService.generateYmlForCandies());
    }

}
