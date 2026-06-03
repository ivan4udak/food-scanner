package com.foodscanner.infrastructure.image;

/** Ошибка обработки изображения. */
public class ImageProcessingException extends RuntimeException {
    public ImageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
