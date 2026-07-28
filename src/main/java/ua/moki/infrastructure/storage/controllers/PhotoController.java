package ua.moki.infrastructure.storage.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ua.moki.infrastructure.storage.service.WatermarkService;

import java.io.IOException;

@RestController
@RequestMapping("/photo")
@RequiredArgsConstructor
public class PhotoController {

    private WatermarkService watermarkService;

    @Autowired
    public PhotoController(WatermarkService watermarkService) {
        this.watermarkService = watermarkService;
    }

    @PostMapping(path = "/watermark",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> upload(@RequestParam("file") MultipartFile file) {
        try {
            watermarkService.addWatermarkToPhoto(file);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }
}
