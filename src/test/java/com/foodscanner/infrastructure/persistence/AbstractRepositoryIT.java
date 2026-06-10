package com.foodscanner.infrastructure.persistence;

import com.foodscanner.application.port.PhotoStorage;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@Transactional
public abstract class AbstractRepositoryIT {

    // Хранилище (MinIO) не нужно repository-IT и недоступно в CI — мок, чтобы MinioPhotoStorage
    // не подключался при старте контекста (init ensure bucket → ConnectException).
    @MockBean
    PhotoStorage photoStorage;

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("foodscanner_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}