package com.foodscanner.infrastructure.image;

import com.foodscanner.application.port.ImageProcessor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Слой: infrastructure
 * Ресайз/сжатие через Thumbnailator (бикубик, читаемость мелкого текста сохраняется).
 * Выход — JPEG. Маленькие изображения НЕ увеличиваются.
 */
@Component
public class ThumbnailatorImageProcessor implements ImageProcessor {

    @Override
    public byte[] resizeToMaxSide(byte[] source, int maxSide, double quality) {
        BufferedImage img = read(source);
        int longSide = Math.max(img.getWidth(), img.getHeight());
        int target   = Math.min(maxSide, longSide);   // не апскейлим
        try (var out = new ByteArrayOutputStream()) {
            Thumbnails.of(img)
                .size(target, target)
                .keepAspectRatio(true)
                .outputFormat("jpg")
                .outputQuality(quality)
                .toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ImageProcessingException("Не удалось обработать изображение", e);
        }
    }

    @Override
    public byte[] thumbnail(byte[] source, int width, double quality) {
        BufferedImage img = read(source);
        int target = Math.min(width, img.getWidth());  // не апскейлим
        try (var out = new ByteArrayOutputStream()) {
            Thumbnails.of(img)
                .width(target)
                .outputFormat("jpg")
                .outputQuality(quality)
                .toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ImageProcessingException("Не удалось создать превью", e);
        }
    }

    private BufferedImage read(byte[] source) {
        try (var in = new ByteArrayInputStream(source)) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) {
                throw new ImageProcessingException("Неподдерживаемый формат изображения", null);
            }
            return img;
        } catch (IOException e) {
            throw new ImageProcessingException("Не удалось прочитать изображение", e);
        }
    }
}
