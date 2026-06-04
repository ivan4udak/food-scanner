package com.foodscanner.application.usecase;

import com.foodscanner.application.command.LoginCommand;
import com.foodscanner.application.command.RecoverPasswordCommand;
import com.foodscanner.application.command.RegisterAccountCommand;
import com.foodscanner.application.result.AuthSession;
import com.foodscanner.application.result.LoginResult;

/** Слой: application. Сценарии входа/создания/восстановления + обновление токена. */
public interface AuthUseCase {
    LoginResult  login(LoginCommand command);
    AuthSession  register(RegisterAccountCommand command);
    AuthSession  recoverPassword(RecoverPasswordCommand command);
    AuthSession  refresh(String refreshToken);
}
