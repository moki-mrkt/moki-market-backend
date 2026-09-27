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
import ua.moki.infrastructure.storage.service.WatermarkService;
import ua.moki.util.ImageConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final S3Client s3Client;
    private final ImageConverter imageConverter;
    private final WatermarkService watermarkService;

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

        byte[] watermarkedBytes = watermarkService.createWatermarkedImageBytes(originalBytes);

        String logoKey = folder + "/" + baseUuid + "_logo.png";
        uploadToS3(logoKey, watermarkedBytes, "image/png");

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
        String baseUuid = UUID.randomUUID().toString();

        String key = folder + "/" + baseUuid +  ".webp";
        byte[] processedImage = imageConverter.resizeAndConvertToWebp(file.getBytes(), ImageConverter.ImageSize.MEDIUM);
        uploadToS3(key, processedImage, "image/webp");

        return key;
    }

    @Override
    public void delete(String key) {
        deleteAllFiles(List.of(key));
    }

    @Override
    public void deleteAllFiles(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        List<ObjectIdentifier> identifiers = keys.stream()
                .flatMap(key -> expandKey(key).stream())
                .map(expandedKey -> ObjectIdentifier.builder().key(expandedKey).build())
                .collect(Collectors.toList());

        Delete delete = Delete.builder()
                .objects(identifiers)
                .build();

        s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(delete)
                .build());
    }

    private List<String> expandKey(String key) {
        if (key != null && key.contains(".")) {
            return List.of(key);
        }

        List<String> expandedKeys = new ArrayList<>();

        for (ImageConverter.ImageSize size : ImageConverter.ImageSize.values()) {
            expandedKeys.add(key + size.suffix + ".webp");
        }

        expandedKeys.add(key + "_logo.png");

        return expandedKeys;
    }

    @Override
    public void generateWatermarksForExistingImages(List<String> imageIds) {
        String sourceSuffix = "_large.webp";

        for (String baseKey : imageIds) {
            String sourceKey = baseKey + sourceSuffix;
            String targetLogoKey = baseKey + "_logo.png";

            try {
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(sourceKey)
                        .build();

                byte[] imageBytes = s3Client.getObject(getObjectRequest).readAllBytes();

                byte[] watermarkedBytes = watermarkService.createWatermarkedImageBytes(imageBytes);

                uploadToS3(targetLogoKey, watermarkedBytes, "image/png");

                log.info("Успішно згенеровано вотермарку для: {}", targetLogoKey);

            } catch (NoSuchKeyException e) {
                log.warn("Пропущено: оригінальне фото не знайдено за ключем {}", sourceKey);
            } catch (Exception e) {
                log.error("Помилка генерації вотермарки для ключа {}: {}", baseKey, e.getMessage());
            }
        }
    }
}
