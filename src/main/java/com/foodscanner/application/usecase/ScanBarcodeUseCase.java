package com.foodscanner.application.usecase;

import com.foodscanner.application.command.ScanBarcodeCommand;
import com.foodscanner.application.result.ScanBarcodeResult;

public interface ScanBarcodeUseCase {
    ScanBarcodeResult execute(ScanBarcodeCommand command);
}
