package ua.moki.infrastructure.storage.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface WatermarkService {

    void addWatermarkToPhoto(MultipartFile inputPhoto) throws IOException;
    byte[] createWatermarkedImageBytes(byte[] originalBytes) throws IOException;

}
