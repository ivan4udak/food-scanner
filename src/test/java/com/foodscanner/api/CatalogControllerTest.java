package com.foodscanner.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodscanner.api.controller.CatalogController;
import com.foodscanner.api.controller.GlobalExceptionHandler;
import com.foodscanner.api.dto.*;
import com.foodscanner.api.mapper.CatalogApiMapper;
import com.foodscanner.application.result.*;
import com.foodscanner.application.result.ScanBarcodeResult.ScanStatus;
import com.foodscanner.application.usecase.*;
import com.foodscanner.domain.exception.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import com.foodscanner.application.port.PhotoStorage;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Слой: api
 * Тип: WebMvcTest (Contract Test)
 *
 * Тестирует HTTP-контракт: статусы, структуру JSON, обработку ошибок.
 * Use case-ы мокируются — тестируем только контроллер и маппер.
 * Spring контекст минимальный — только web слой.
 */
@WebMvcTest(CatalogController.class)
@Import({CatalogApiMapper.class, GlobalExceptionHandler.class})
@DisplayName("CatalogController — Contract Tests")
class CatalogControllerTest {

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean RegisterContributorUseCase      registerContributor;
    @MockBean ScanBarcodeUseCase              scanBarcode;
    @MockBean AddDraftPhotoUseCase            addDraftPhoto;
    @MockBean CompleteCatalogUseCase          completeCatalog;
    @MockBean FindCatalogEntryByBarcodeUseCase findByBarcode;
    @MockBean PhotoStorage                     photoStorage;

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/contributors")
    class RegisterContributor {

        @Test
        @DisplayName("201 при успешной регистрации")
        void shouldReturn201OnSuccess() throws Exception {
            UUID id = UUID.randomUUID();
            when(registerContributor.execute(any()))
                .thenReturn(new RegisterContributorResult(id, "alice"));

            mockMvc.perform(post("/api/v1/contributors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"nickname": "alice"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contributorId").value(id.toString()))
                .andExpect(jsonPath("$.nickname").value("alice"));
        }

        @Test
        @DisplayName("400 если nickname пустой")
        void shouldReturn400WhenNicknameBlank() throws Exception {
            mockMvc.perform(post("/api/v1/contributors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"nickname": ""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("409 если nickname занят")
        void shouldReturn409WhenNicknameTaken() throws Exception {
            when(registerContributor.execute(any()))
                .thenThrow(new ContributorAlreadyExistsException("alice"));

            mockMvc.perform(post("/api/v1/contributors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"nickname": "alice"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        }
    }

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/scan")
    class ScanBarcode {

        @Test
        @DisplayName("200 status=NEW с draftId при новом штрихкоде")
        void shouldReturnNewWithDraftId() throws Exception {
            UUID draftId = UUID.randomUUID();
            when(scanBarcode.execute(any()))
                .thenReturn(ScanBarcodeResult.newProduct(draftId));

            mockMvc.perform(post("/api/v1/scan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        new ScanBarcodeRequest("4607038310042", UUID.randomUUID()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.draftId").value(draftId.toString()));
        }

        @Test
        @DisplayName("200 status=EXISTS без draftId при существующем штрихкоде")
        void shouldReturnExistsWithoutDraftId() throws Exception {
            when(scanBarcode.execute(any()))
                .thenReturn(ScanBarcodeResult.alreadyExists());

            mockMvc.perform(post("/api/v1/scan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        new ScanBarcodeRequest("4607038310042", UUID.randomUUID()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXISTS"))
                .andExpect(jsonPath("$.draftId").doesNotExist());
        }

        @Test
        @DisplayName("400 если barcodeValue пустой")
        void shouldReturn400WhenBarcodeBlank() throws Exception {
            mockMvc.perform(post("/api/v1/scan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        new ScanBarcodeRequest("", UUID.randomUUID()))))
                .andExpect(status().isBadRequest());
        }
    }

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/drafts/{draftId}/photos")
    class AddDraftPhoto {

        private MockMultipartFile photo() {
            return new MockMultipartFile(
                "file", "front.jpg", "image/jpeg", new byte[]{1, 2, 3});
        }

        @Test
        @DisplayName("200 с прогрессом после загрузки фото (multipart)")
        void shouldReturnProgressAfterAdd() throws Exception {
            when(photoStorage.upload(any(), any(), any())).thenReturn("drafts/x/front/u.jpg");
            when(addDraftPhoto.execute(any()))
                .thenReturn(new AddDraftPhotoResult(1, 4,
                    Set.of(com.foodscanner.domain.model.PhotoType.BARCODE,
                           com.foodscanner.domain.model.PhotoType.INGREDIENTS,
                           com.foodscanner.domain.model.PhotoType.NUTRITION),
                    false));

            mockMvc.perform(multipart("/api/v1/drafts/{draftId}/photos", UUID.randomUUID())
                    .file(photo())
                    .param("contributorId", UUID.randomUUID().toString())
                    .param("photoType", "FRONT")
                    .param("capturedAt", "2026-05-01T10:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadedCount").value(1))
                .andExpect(jsonPath("$.requiredCount").value(4))
                .andExpect(jsonPath("$.complete").value(false));
        }

        @Test
        @DisplayName("200 complete=true когда все обязательные загружены")
        void shouldReturnCompleteTrueWhenAllUploaded() throws Exception {
            when(photoStorage.upload(any(), any(), any())).thenReturn("drafts/x/nutrition/u.jpg");
            when(addDraftPhoto.execute(any()))
                .thenReturn(new AddDraftPhotoResult(4, 4, Set.of(), true));

            mockMvc.perform(multipart("/api/v1/drafts/{draftId}/photos", UUID.randomUUID())
                    .file(photo())
                    .param("contributorId", UUID.randomUUID().toString())
                    .param("photoType", "NUTRITION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complete").value(true))
                .andExpect(jsonPath("$.uploadedCount").value(4));
        }

        @Test
        @DisplayName("404 если черновик не найден")
        void shouldReturn404WhenDraftNotFound() throws Exception {
            UUID draftId = UUID.randomUUID();
            when(photoStorage.upload(any(), any(), any())).thenReturn("drafts/x/front/u.jpg");
            when(addDraftPhoto.execute(any()))
                .thenThrow(new CatalogDraftNotFoundException(draftId));

            mockMvc.perform(multipart("/api/v1/drafts/{draftId}/photos", draftId)
                    .file(photo())
                    .param("contributorId", UUID.randomUUID().toString())
                    .param("photoType", "FRONT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("400 при невалидном PhotoType")
        void shouldReturn400WhenPhotoTypeInvalid() throws Exception {
            when(photoStorage.upload(any(), any(), any())).thenReturn("drafts/x/u.jpg");

            mockMvc.perform(multipart("/api/v1/drafts/{draftId}/photos", UUID.randomUUID())
                    .file(photo())
                    .param("contributorId", UUID.randomUUID().toString())
                    .param("photoType", "INVALID_TYPE"))
                .andExpect(status().isBadRequest());
        }
    }

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/drafts/{draftId}/complete")
    class CompleteCatalog {

        @Test
        @DisplayName("201 с catalogEntryId при успешном завершении")
        void shouldReturn201OnSuccess() throws Exception {
            UUID entryId = UUID.randomUUID();
            when(completeCatalog.execute(any()))
                .thenReturn(new CompleteCatalogResult(entryId, 1));

            mockMvc.perform(post("/api/v1/drafts/{draftId}/complete", UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        new CompleteCatalogRequest(UUID.randomUUID()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.catalogEntryId").value(entryId.toString()))
                .andExpect(jsonPath("$.contributorCompletedCount").value(1));
        }

        @Test
        @DisplayName("422 с missingTypes если не все фото загружены")
        void shouldReturn422WithMissingTypes() throws Exception {
            when(completeCatalog.execute(any()))
                .thenThrow(new CatalogNotCompletableException(
                    Set.of(com.foodscanner.domain.model.PhotoType.NUTRITION,
                           com.foodscanner.domain.model.PhotoType.EXTRA)));

            mockMvc.perform(post("/api/v1/drafts/{draftId}/complete", UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        new CompleteCatalogRequest(UUID.randomUUID()))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.details").isArray());
        }

        @Test
        @DisplayName("404 если черновик не найден")
        void shouldReturn404WhenDraftNotFound() throws Exception {
            UUID draftId = UUID.randomUUID();
            when(completeCatalog.execute(any()))
                .thenThrow(new CatalogDraftNotFoundException(draftId));

            mockMvc.perform(post("/api/v1/drafts/{draftId}/complete", draftId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        new CompleteCatalogRequest(UUID.randomUUID()))))
                .andExpect(status().isNotFound());
        }
    }

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v1/entries/{barcode}")
    class FindByBarcode {

        @Test
        @DisplayName("200 с данными если запись найдена")
        void shouldReturn200WhenFound() throws Exception {
            UUID entryId       = UUID.randomUUID();
            UUID contributorId = UUID.randomUUID();

            when(findByBarcode.execute("4607038310042"))
                .thenReturn(new FindCatalogEntryResult(
                    entryId, "4607038310042", contributorId,
                    List.of(new FindCatalogEntryResult.PhotoInfo(
                        UUID.randomUUID(), "FRONT", "drafts/front.jpg", Instant.now())),
                    Instant.now()));

            mockMvc.perform(get("/api/v1/entries/{barcode}", "4607038310042"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barcode").value("4607038310042"))
                .andExpect(jsonPath("$.photos").isArray())
                .andExpect(jsonPath("$.photos[0].type").value("FRONT"));
        }

        @Test
        @DisplayName("404 если запись не найдена")
        void shouldReturn404WhenNotFound() throws Exception {
            when(findByBarcode.execute("0000000000000")).thenReturn(null);

            mockMvc.perform(get("/api/v1/entries/{barcode}", "0000000000000"))
                .andExpect(status().isNotFound());
        }
    }
}
