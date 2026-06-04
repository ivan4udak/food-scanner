package com.foodscanner.application.usecase;

import com.foodscanner.application.command.AdminResetPasswordCommand;

/** Слой: application. Админские операции. */
public interface AdminUseCase {
    void resetPassword(AdminResetPasswordCommand command);
}
