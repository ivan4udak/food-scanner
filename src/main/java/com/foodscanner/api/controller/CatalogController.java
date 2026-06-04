package com.foodscanner.api.controller;

import com.foodscanner.api.dto.*;
import com.foodscanner.api.mapper.CatalogApiMapper;
import com.foodscanner.application.port.ImageProcessor;
import com.foodscanner.application.port.PhotoStorage;
import com.foodscanner.application.result.FindCatalogEntryResult;
import com.foodscanner.application.usecase.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: api
 *
 * Зачем: HTTP-граница приложения. Знает только об use case интерфейсах
 * и DTO — не о сервисах, репозиториях или JPA-сущностях.
 *
 * Зависимости: UseCase интерфейсы (application), CatalogApiMapper, DTO.
 *
 * Эндпоинты:
 *   POST /api/v1/contributors              — регистрация контрибьютора
 *   POST /api/v1/scan                      — сканирование штрихкода
 *   POST /api/v1/drafts/{draftId}/photos   — добавление фото в черновик
 *   POST /api/v1/drafts/{draftId}/complete — завершение каталога
 *   GET  /api/v1/entries/{barcode}         — поиск записи по штрихкоду
 */
@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final RegisterContributorUseCase      registerContributor;
    private final ScanBarcodeUseCase              scanBarcode;
    private final AddDraftPhotoUseCase            addDraftPhoto;
    private final CompleteCatalogUseCase          completeCatalog;
    private final FindCatalogEntryByBarcodeUseCase findByBarcode;
    private final CatalogApiMapper                mapper;
    private final PhotoStorage                    photoStorage;
    private final ImageProcessor                  imageProcessor;
    private final com.foodscanner.application.port.PhotoStore photoStore;

    /** Имя request-атрибута с id аутентифицированного пользователя (ставит AuthInterceptor). */
    private static final String AUTH_CONTRIBUTOR = "authContributorId";

    /** Full ≤ 1920 по большей стороне (≤ 2K), thumbnail ~144px. Оригинал не хранится. */
    private static final int    FULL_MAX_SIDE = 1920;
    private static final int    THUMB_WIDTH   = 144;
    private static final double FULL_QUALITY  = 0.85;
    private static final double THUMB_QUALITY = 0.8;

    public CatalogController(
            RegisterContributorUseCase registerContributor,
            ScanBarcodeUseCase scanBarcode,
            AddDraftPhotoUseCase addDraftPhoto,
            CompleteCatalogUseCase completeCatalog,
            FindCatalogEntryByBarcodeUseCase findByBarcode,
            CatalogApiMapper mapper,
            PhotoStorage photoStorage,
            ImageProcessor imageProcessor,
            com.foodscanner.application.port.PhotoStore photoStore) {
        this.registerContributor = registerContributor;
        this.scanBarcode         = scanBarcode;
        this.addDraftPhoto       = addDraftPhoto;
        this.completeCatalog     = completeCatalog;
        this.findByBarcode       = findByBarcode;
        this.mapper              = mapper;
        this.photoStorage        = photoStorage;
        this.imageProcessor      = imageProcessor;
        this.photoStore          = photoStore;
    }

    /**
     * POST /api/v1/contributors
     * Регистрирует нового участника каталогизации.
     * 201 Created — успех
     * 409 Conflict — nickname занят
     */
    @PostMapping("/contributors")
    public ResponseEntity<RegisterContributorResponse> registerContributor(
            @Valid @RequestBody RegisterContributorRequest request) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(mapper.toResponse(
                registerContributor.execute(mapper.toCommand(request))));
    }

    /**
     * POST /api/v1/scan
     * Сканирует штрихкод.
     * 200 OK — status=NEW (зелёный экран, draftId в ответе)
     * 200 OK — status=EXISTS (красный экран, draftId=null)
     */
    @PostMapping("/scan")
    public ResponseEntity<ScanBarcodeResponse> scan(
            @Valid @RequestBody ScanBarcodeRequest request,
            @RequestAttribute(AUTH_CONTRIBUTOR) UUID contributorId) {
        return ResponseEntity.ok(
            mapper.toResponse(scanBarcode.execute(
                new com.foodscanner.application.command.ScanBarcodeCommand(
                    request.getBarcodeValue(), contributorId))));
    }

    /**
     * POST /api/v1/drafts/{draftId}/photos  (multipart/form-data)
     * Загружает БИНАРНОЕ фото в объектное хранилище и регистрирует его в черновике.
     * Части: file (бинарь), contributorId, photoType, capturedAt (ISO-8601, опц.).
     * 200 OK — с прогрессом (uploadedCount/requiredCount, missingTypes)
     * 404 Not Found — черновик не найден
     * 422 Unprocessable Entity — чужой черновик или COMPLETED/ABANDONED
     */
    @PostMapping(value = "/drafts/{draftId}/photos",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AddDraftPhotoResponse> addPhoto(
            @PathVariable UUID draftId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("photoType") String photoType,
            @RequestParam(value = "capturedAt", required = false) String capturedAt,
            @RequestAttribute(AUTH_CONTRIBUTOR) UUID contributorId) throws Exception {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        // Block 7: оригинал не храним. Делаем Full (≤1920) и Thumbnail (~144), обе в JPEG.
        byte[] original = file.getBytes();
        byte[] full  = imageProcessor.resizeToMaxSide(original, FULL_MAX_SIDE, FULL_QUALITY);
        byte[] thumb = imageProcessor.thumbnail(original, THUMB_WIDTH, THUMB_QUALITY);

        // Block 16: дедупликация по SHA-256 — контент-адресный ключ, без повторной заливки.
        String objectKey = photoStore.store(full, thumb, "image/jpeg");

        Instant captured = parseInstant(capturedAt);

        return ResponseEntity.ok(
            mapper.toResponse(
                addDraftPhoto.execute(
                    mapper.toCommand(draftId, contributorId, photoType, objectKey, captured))));
    }

    /**
     * GET /api/v1/photos/{objectKey...}?size=thumb|full
     * Отдаёт фото из хранилища. По умолчанию full; size=thumb — превью ~144px.
     */
    @GetMapping("/photos/{*objectKey}")
    public ResponseEntity<byte[]> getPhoto(
            @PathVariable String objectKey,
            @RequestParam(name = "size", defaultValue = "full") String size) {
        String key = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        if ("thumb".equalsIgnoreCase(size)) {
            key = thumbKey(key);
        }
        PhotoStorage.StoredObject obj = photoStorage.download(key);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(
                obj.contentType() != null ? obj.contentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .body(obj.content());
    }

    /** Ключ thumbnail: вставляет _thumb перед расширением. */
    private static String thumbKey(String key) {
        int dot = key.lastIndexOf('.');
        return dot < 0 ? key + "_thumb" : key.substring(0, dot) + "_thumb" + key.substring(dot);
    }

    private static Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Instant.parse(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * POST /api/v1/drafts/{draftId}/complete
     * Завершает черновик и создаёт CatalogEntry.
     * 201 Created — успех, catalogEntryId в ответе
     * 404 Not Found — черновик не найден
     * 422 Unprocessable Entity — не все фото загружены (список missingTypes в ErrorResponse)
     */
    @PostMapping("/drafts/{draftId}/complete")
    public ResponseEntity<CompleteCatalogResponse> completeCatalog(
            @PathVariable UUID draftId,
            @RequestAttribute(AUTH_CONTRIBUTOR) UUID contributorId) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(mapper.toResponse(
                completeCatalog.execute(
                    new com.foodscanner.application.command.CompleteCatalogCommand(draftId, contributorId))));
    }

    /**
     * GET /api/v1/entries/{barcode}
     * Ищет запись каталога по штрихкоду.
     * 200 OK — запись найдена
     * 404 Not Found — не найдена
     */
    @GetMapping("/entries/{barcode}")
    public ResponseEntity<CatalogEntryResponse> findByBarcode(
            @PathVariable String barcode) {
        FindCatalogEntryResult result = findByBarcode.execute(barcode);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.toResponse(result));
    }
}
