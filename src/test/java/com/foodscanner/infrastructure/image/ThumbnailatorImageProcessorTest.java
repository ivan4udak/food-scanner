package com.foodscanner.infrastructure.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ThumbnailatorImageProcessor")
class ThumbnailatorImageProcessorTest {

    private final ThumbnailatorImageProcessor proc = new ThumbnailatorImageProcessor();

    private byte[] image(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private int[] dimensions(byte[] data) throws Exception {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
        return new int[]{img.getWidth(), img.getHeight()};
    }

    @Test
    @DisplayName("resizeToMaxSide уменьшает большую сторону до 1920, сохраняя пропорции")
    void resizesLargeImage() throws Exception {
        byte[] full = proc.resizeToMaxSide(image(4000, 3000), 1920, 0.85);
        int[] d = dimensions(full);
        assertTrue(Math.max(d[0], d[1]) <= 1920, "большая сторона ≤ 1920");
        // 4000x3000 → 1920x1440 (пропорция 4:3)
        assertEquals(1920, d[0]);
        assertEquals(1440, d[1]);
    }

    @Test
    @DisplayName("resizeToMaxSide не увеличивает маленькое изображение")
    void doesNotUpscale() throws Exception {
        byte[] full = proc.resizeToMaxSide(image(800, 600), 1920, 0.85);
        int[] d = dimensions(full);
        assertTrue(d[0] <= 800 && d[1] <= 600);
    }

    @Test
    @DisplayName("thumbnail делает ширину 144")
    void thumbnailWidth() throws Exception {
        byte[] thumb = proc.thumbnail(image(4000, 3000), 144, 0.8);
        int[] d = dimensions(thumb);
        assertEquals(144, d[0]);
    }
}
