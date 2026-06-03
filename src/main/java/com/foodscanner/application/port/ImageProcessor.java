package com.foodscanner.application.port;

/**
 * Слой: application (порт)
 * Обработка изображений перед сохранением. Реализация (Thumbnailator) — в infrastructure.
 *
 * Оригиналы не хранятся: сервер всегда уменьшает до Full (≤ maxSide) и делает
 * отдельный thumbnail. Выходной формат — JPEG.
 */
public interface ImageProcessor {

    /** Уменьшает до квадрата maxSide по большей стороне (если меньше — не увеличивает). JPEG. */
    byte[] resizeToMaxSide(byte[] source, int maxSide, double quality);

    /** Превью заданной ширины (высота пропорционально). JPEG. */
    byte[] thumbnail(byte[] source, int width, double quality);
}
