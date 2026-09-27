package ua.moki.infrastructure.storage.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import ua.moki.infrastructure.storage.service.WatermarkService;
import ua.moki.modules.sender.services.TelegramSenderService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class WatermarkServiceImpl implements WatermarkService {

    @Autowired
    private TelegramSenderService telegramSenderService;

    private static final String LOGO_PATH = "src/main/resources/static/logo.png";
    private static final int TARGET_WIDTH = 1000;
    private static final int TARGET_HEIGHT = 1000;

    @Override
    public void addWatermarkToPhoto(MultipartFile inputPhoto) throws IOException {
        if (inputPhoto == null || inputPhoto.isEmpty()) {
            throw new IOException("Файл порожній або відсутній");
        }

        BufferedImage watermarkedImage = applyWatermark(inputPhoto.getBytes());

        Path tempFile = Files.createTempFile("product_", ".png");
        try {
            ImageIO.write(watermarkedImage, "png", tempFile.toFile());
            telegramSenderService.sendPhotoToTelegram(tempFile.toFile());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Override
    public byte[] createWatermarkedImageBytes(byte[] originalBytes) throws IOException {
        BufferedImage watermarkedImage = applyWatermark(originalBytes);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(watermarkedImage, "png", baos);
            return baos.toByteArray();
        }
    }

    private BufferedImage applyWatermark(byte[] imageBytes) throws IOException {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));

        if (originalImage == null) {
            throw new IOException("Не вдалося прочитати зображення. Можливо, непідтримуваний формат.");
        }

        BufferedImage watermarkedImage = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = watermarkedImage.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        boolean drawn = g2d.drawImage(originalImage, 0, 0, TARGET_WIDTH, TARGET_HEIGHT, null);
        if (!drawn) {
            System.out.println("Помилка: Основне фото не було намальоване на полотні");
        }

        BufferedImage logo = ImageIO.read(new File(LOGO_PATH));
        if (logo != null) {
            int padding = 80;
            int x = TARGET_WIDTH - logo.getWidth() - padding;
            int y = TARGET_HEIGHT - logo.getHeight() - padding;
            g2d.drawImage(logo, x, y, null);
        }

        g2d.dispose();

        return watermarkedImage;
    }
}
