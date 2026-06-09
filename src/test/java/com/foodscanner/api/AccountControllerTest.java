package com.foodscanner.api;

import com.foodscanner.api.controller.AccountController;
import com.foodscanner.api.controller.GlobalExceptionHandler;
import com.foodscanner.application.usecase.SetLeaderboardVisibilityUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AccountController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {com.foodscanner.infrastructure.config.WebConfig.class,
                   com.foodscanner.infrastructure.security.AuthInterceptor.class}))
@Import(GlobalExceptionHandler.class)
@DisplayName("AccountController — Contract Tests")
class AccountControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean SetLeaderboardVisibilityUseCase setVisibility;

    @Test
    @DisplayName("POST /me/leaderboard-visibility — 200 с новым состоянием")
    void setsVisibility() throws Exception {
        UUID contributor = UUID.randomUUID();
        when(setVisibility.execute(eq(contributor), eq(true))).thenReturn(true);

        mockMvc.perform(post("/api/v1/me/leaderboard-visibility")
                .requestAttr("authContributorId", contributor)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hidden\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hiddenFromLeaderboard").value(true));
    }

    @Test
    @DisplayName("POST без поля hidden — 400")
    void validation() throws Exception {
        mockMvc.perform(post("/api/v1/me/leaderboard-visibility")
                .requestAttr("authContributorId", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}
