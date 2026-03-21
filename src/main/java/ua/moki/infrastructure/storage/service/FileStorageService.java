package ua.moki.infrastructure.storage.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {
    String uploadProductImage(MultipartFile file);
    String uploadUserPhoto(MultipartFile file, String folder);
    void delete(String key);
    void deleteAllFiles(List<String> keys);
}
