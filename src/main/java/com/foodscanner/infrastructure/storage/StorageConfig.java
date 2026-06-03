package com.foodscanner.infrastructure.storage;

import com.foodscanner.application.port.PhotoStorage;
import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Слой: infrastructure
 * Подключает реализацию PhotoStorage по свойству storage.provider.
 * Сейчас реализован MinIO; S3 добавляется новым @Bean без изменения use case.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    public MinioClient minioClient(StorageProperties props) {
        StorageProperties.Minio m = props.getMinio();
        return MinioClient.builder()
            .endpoint(m.getEndpoint())
            .credentials(m.getAccessKey(), m.getSecretKey())
            .build();
    }

    @Bean
    public PhotoStorage photoStorage(MinioClient client, StorageProperties props) {
        return new MinioPhotoStorage(client, props.getMinio().getBucket());
    }
}
