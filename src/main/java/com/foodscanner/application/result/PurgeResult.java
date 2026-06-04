package com.foodscanner.application.result;

/** Итог очистки мусора: сколько черновиков и объектов хранилища удалено. */
public record PurgeResult(int draftsDeleted, int objectsDeleted) {}
