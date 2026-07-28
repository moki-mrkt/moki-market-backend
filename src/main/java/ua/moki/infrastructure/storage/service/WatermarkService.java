package ua.moki.infrastructure.storage.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface WatermarkService {

    void addWatermarkToPhoto(MultipartFile inputPhoto) throws IOException;
}
