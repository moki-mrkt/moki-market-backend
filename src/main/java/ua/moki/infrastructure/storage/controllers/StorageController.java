package ua.moki.infrastructure.storage.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ua.moki.infrastructure.storage.service.FileStorageService;

import java.util.Map;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
public class StorageController {

    private final FileStorageService fileStorageService;

    @PostMapping
//    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    @SecurityRequirements()
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("folder") String folder
    ) {
        String key = fileStorageService.upload(file, folder);

        // Повертаємо JSON { "key": "products/123.jpg", "url": "..." }
        return ResponseEntity.ok(Map.of(
                "key", key,
                "url", "http://localhost:9000/moki-images/" + key
        ));
    }

    @DeleteMapping
//    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    @SecurityRequirements()
    public ResponseEntity<Void> deleteFile(@RequestParam("key") String key) {
        fileStorageService.delete(key);
        return ResponseEntity.ok().build();
    }
}
