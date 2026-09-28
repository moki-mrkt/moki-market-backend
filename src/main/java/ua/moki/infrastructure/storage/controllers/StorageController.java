package ua.moki.infrastructure.storage.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ua.moki.infrastructure.storage.service.FileStorageService;

import java.util.Map;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
public class StorageController {

    @Value("${s3.public_url}")
    private String urlBucket;
    private final FileStorageService fileStorageService;


    @PostMapping("/product")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> uploadProductPhoto(@RequestParam("file") MultipartFile file) {

        String imageId = fileStorageService.uploadProductImage(file);

        return ResponseEntity.ok(Map.of(
                "imageId", imageId,
                "url", urlBucket + imageId + "_medium.webp"
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    public ResponseEntity<Map<String, String>> uploadUserPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("folder") String folder
    ) {
        String key = fileStorageService.uploadUserPhoto(file, folder);

        return ResponseEntity.ok(Map.of(
                "key", key,
                "url", urlBucket +  key
        ));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    public ResponseEntity<Void> deleteFile(@RequestParam("key") String key) {
        fileStorageService.delete(key);
        return ResponseEntity.ok().build();
    }
}
