package com.foodscanner.application.usecase;

import com.foodscanner.application.command.LoginCommand;
import com.foodscanner.application.command.RecoverPasswordCommand;
import com.foodscanner.application.command.RegisterAccountCommand;
import com.foodscanner.application.result.AccountResult;
import com.foodscanner.application.result.LoginResult;

/** Слой: application. Сценарии входа/создания/восстановления. */
public interface AuthUseCase {
    LoginResult   login(LoginCommand command);
    AccountResult register(RegisterAccountCommand command);
    AccountResult recoverPassword(RecoverPasswordCommand command);
}
