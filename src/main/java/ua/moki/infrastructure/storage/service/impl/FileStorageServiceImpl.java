package ua.moki.infrastructure.storage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import ua.moki.infrastructure.storage.service.FileStorageService;
import ua.moki.util.ImageConverter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final S3Client s3Client;
    private final ImageConverter imageConverter;

    @Value("${s3.bucket}")
    private String bucket;

    @Override
    @SneakyThrows
    public String uploadProductImage(MultipartFile file) {
        String folder = "products";
        String baseUuid = UUID.randomUUID().toString();
        byte[] originalBytes = file.getBytes();

        for (ImageConverter.ImageSize size : ImageConverter.ImageSize.values()) {
            byte[] processedImage = imageConverter.resizeAndConvertToWebp(originalBytes, size);
            String key = folder + "/" + baseUuid + size.suffix + ".webp";
            uploadToS3(key, processedImage, "image/webp");
        }

        return folder + "/" + baseUuid;
    }

    private void uploadToS3(String key, byte[] bytes, String contentType) {
        PutObjectRequest putOb = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putOb, RequestBody.fromBytes(bytes));
    }

    @Override
    @SneakyThrows
    public String uploadUserPhoto(MultipartFile file, String folder) {
        String key = folder + "/" + UUID.randomUUID() + getExtension(file.getOriginalFilename());

        uploadToS3(key, file.getBytes(), file.getContentType());
        return key;
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    @Override
    public void deleteAllFiles(List<String> keys) {

        List<ObjectIdentifier> identifiers = keys.stream()
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .collect(Collectors.toList());

        Delete delete = Delete.builder()
                .objects(identifiers)
                .build();

        s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(delete)
                .build());
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }
}
