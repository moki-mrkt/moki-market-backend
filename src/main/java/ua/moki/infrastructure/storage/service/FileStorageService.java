package ua.moki.infrastructure.storage.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String upload(MultipartFile file, String folder);
    void delete(String key);
}
