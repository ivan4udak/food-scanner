package com.foodscanner.application.usecase;

import com.foodscanner.application.command.RegisterContributorCommand;
import com.foodscanner.application.result.RegisterContributorResult;

public interface RegisterContributorUseCase {
    RegisterContributorResult execute(RegisterContributorCommand command);
}
