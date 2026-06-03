package com.foodscanner.api.mapper;

import com.foodscanner.api.dto.*;
import com.foodscanner.application.command.*;
import com.foodscanner.application.result.*;
import com.foodscanner.domain.model.PhotoType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Слой: api
 *
 * Зачем: маппинг Request DTO → Command и Result → Response DTO.
 * Контроллер не знает о domain объектах, use case не знает об HTTP.
 * Конвертация PhotoType String → enum происходит здесь — если строка
 * невалидна, IllegalArgumentException поймает GlobalExceptionHandler.
 *
 * Зависимости: application.command, application.result, api.dto.
 * Нет зависимости от domain напрямую (только через PhotoType enum).
 */
@Component
public class CatalogApiMapper {

    // ── Request → Command ──────────────────────────

    public RegisterContributorCommand toCommand(RegisterContributorRequest req) {
        return new RegisterContributorCommand(req.getNickname());
    }

    public ScanBarcodeCommand toCommand(ScanBarcodeRequest req) {
        return new ScanBarcodeCommand(req.getBarcodeValue(), req.getContributorId());
    }

    public AddDraftPhotoCommand toCommand(java.util.UUID draftId,
                                          AddDraftPhotoRequest req) {
        PhotoType type = PhotoType.valueOf(req.getPhotoType().toUpperCase());
        return new AddDraftPhotoCommand(
            draftId, req.getContributorId(), type, req.getStorageKey());
    }

    public CompleteCatalogCommand toCommand(java.util.UUID draftId,
                                            CompleteCatalogRequest req) {
        return new CompleteCatalogCommand(draftId, req.getContributorId());
    }

    // ── Result → Response ──────────────────────────

    public RegisterContributorResponse toResponse(RegisterContributorResult result) {
        return new RegisterContributorResponse(
            result.getContributorId(), result.getNickname());
    }

    public ScanBarcodeResponse toResponse(ScanBarcodeResult result) {
        return new ScanBarcodeResponse(
            result.getStatus().name(), result.getDraftId());
    }

    public AddDraftPhotoResponse toResponse(AddDraftPhotoResult result) {
        return new AddDraftPhotoResponse(
            result.getUploadedCount(),
            result.getRequiredCount(),
            result.getMissingTypes().stream()
                .map(Enum::name)
                .collect(Collectors.toSet()),
            result.isComplete());
    }

    public CompleteCatalogResponse toResponse(CompleteCatalogResult result) {
        return new CompleteCatalogResponse(
            result.getCatalogEntryId(),
            result.getContributorCompletedCount());
    }

    public CatalogEntryResponse toResponse(FindCatalogEntryResult result) {
        List<CatalogEntryResponse.PhotoDto> photos = result.getPhotos().stream()
            .map(p -> new CatalogEntryResponse.PhotoDto(p.id(), p.type(), p.storageKey()))
            .collect(Collectors.toList());

        return new CatalogEntryResponse(
            result.getId(), result.getBarcode(),
            result.getContributorId(), photos, result.getCreatedAt());
    }
}
