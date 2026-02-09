package ua.moki.infrastructure.storage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import ua.moki.infrastructure.storage.service.FileStorageService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    @Autowired
    private final S3Client s3Client;

    @Value("${s3.bucket}")
    private String bucket;

    @Override
    @SneakyThrows
    public String upload(MultipartFile file, String folder) {

        String extension = getExtension(file.getOriginalFilename());
        String key = folder + "/" + UUID.randomUUID() + extension;

        PutObjectRequest putOb = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                // .acl(ObjectCannedACL.PUBLIC_READ) // Для MinIO не обов'язково, бо бакет публічний
                .build();

        s3Client.putObject(putOb, RequestBody.fromBytes(file.getBytes()));

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
