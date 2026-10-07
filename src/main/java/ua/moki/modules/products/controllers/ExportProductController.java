package ua.moki.modules.products.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.services.exports.KastaExport;
import ua.moki.modules.products.services.exports.PromExport;
import ua.moki.modules.products.services.exports.RozetkaExport;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportProductController {

    private final PromExport promExport;
    private final RozetkaExport rozetkaExport;
    private final KastaExport kastaExport;

    @GetMapping(value = "/rozetka", produces = MediaType.APPLICATION_XML_VALUE)
    @PreAuthorize("permitAll()")
    @SecurityRequirements()
    public ResponseEntity<String> rozetkaCandies(@RequestParam ProductCategory category, @RequestParam String subcategory) {
        return ResponseEntity.ok(rozetkaExport.generate(category, subcategory));
    }

    @GetMapping(value = "/kasta", produces = MediaType.APPLICATION_XML_VALUE)
    @PreAuthorize("permitAll()")
    @SecurityRequirements()
    public ResponseEntity<String> kastaCandies(@RequestParam ProductCategory category, @RequestParam String subcategory) {
        return ResponseEntity.ok(kastaExport.generate(category, subcategory));
    }

    @GetMapping(value = "/prom", produces = MediaType.APPLICATION_XML_VALUE)
    @PreAuthorize("permitAll()")
    @SecurityRequirements()
    public ResponseEntity<String> promSweets(@RequestParam ProductCategory category, @RequestParam String subcategory) {
        return ResponseEntity.ok(promExport.generate(category, subcategory));
    }

}
