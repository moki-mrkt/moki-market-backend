package ua.moki.util;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ImageConverter {

    public enum ImageSize {
        THUMB(300, 300, "_thumb"),
        MEDIUM(800, 800, "_medium"),
        LARGE(1200, 1200, "_large");

        final int width;
        final int height;
        public final String suffix;

        ImageSize(int width, int height, String suffix) {
            this.width = width;
            this.height = height;
            this.suffix = suffix;
        }
    }

    public byte[] resizeAndConvertToWebp(byte[] bytes, ImageSize size) throws IOException {
        return ImmutableImage.loader()
                .fromBytes(bytes)
                .max(size.width, size.height)
                .bytes(WebpWriter.DEFAULT.withQ(75));
    }
}