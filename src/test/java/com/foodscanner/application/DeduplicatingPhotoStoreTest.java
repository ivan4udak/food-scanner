package com.foodscanner.application;

import com.foodscanner.application.port.PhotoStorage;
import com.foodscanner.application.service.DeduplicatingPhotoStore;
import com.foodscanner.domain.repository.PhotoObjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("DeduplicatingPhotoStore — SHA-256 дедупликация")
class DeduplicatingPhotoStoreTest {

    private PhotoStorage storage;
    private PhotoObjectRepository registry;
    private DeduplicatingPhotoStore store;

    private final byte[] full  = "FULL-CONTENT".getBytes();
    private final byte[] thumb = "THUMB".getBytes();

    @BeforeEach
    void setUp() {
        storage = mock(PhotoStorage.class);
        registry = mock(PhotoObjectRepository.class);
        when(storage.upload(any(), any(), any())).thenAnswer(i -> i.getArgument(2));
        store = new DeduplicatingPhotoStore(storage, registry);
    }

    @Test
    @DisplayName("новый контент → заливает full+thumb и регистрирует hash")
    void newContentUploads() {
        when(registry.findObjectKeyByHash(any())).thenReturn(Optional.empty());

        String key = store.store(full, thumb, "image/jpeg");

        assertTrue(key.startsWith("photos/"));
        assertTrue(key.endsWith(".jpg"));
        verify(storage).upload(eq(full), eq("image/jpeg"), eq(key));
        verify(storage).upload(eq(thumb), eq("image/jpeg"), eq(DeduplicatingPhotoStore.thumbKey(key)));
        verify(registry).register(any(), eq(key));
    }

    @Test
    @DisplayName("дубликат → переиспользует существующий объект, без заливки")
    void duplicateReuses() {
        when(registry.findObjectKeyByHash(any())).thenReturn(Optional.of("photos/existing.jpg"));

        String key = store.store(full, thumb, "image/jpeg");

        assertEquals("photos/existing.jpg", key);
        verify(storage, never()).upload(any(), any(), any());
        verify(registry, never()).register(any(), any());
    }

    @Test
    @DisplayName("одинаковый контент → одинаковый ключ (детерминированный hash)")
    void sameContentSameKey() {
        when(registry.findObjectKeyByHash(any())).thenReturn(Optional.empty());
        String k1 = store.store(full, thumb, "image/jpeg");
        String k2 = store.store(full, thumb, "image/jpeg");
        assertEquals(k1, k2);
    }
}
