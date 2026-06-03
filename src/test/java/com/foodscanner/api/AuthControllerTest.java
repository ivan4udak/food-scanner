package com.foodscanner.api;

import com.foodscanner.api.controller.AdminController;
import com.foodscanner.api.controller.AuthController;
import com.foodscanner.api.controller.GlobalExceptionHandler;
import com.foodscanner.api.controller.PingController;
import com.foodscanner.application.result.AccountResult;
import com.foodscanner.application.result.LoginResult;
import com.foodscanner.application.usecase.AdminUseCase;
import com.foodscanner.application.usecase.AuthUseCase;
import com.foodscanner.domain.exception.ContributorAlreadyExistsException;
import com.foodscanner.domain.exception.InvalidAdminCredentialsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuthController.class, AdminController.class, PingController.class})
@Import(GlobalExceptionHandler.class)
@DisplayName("Auth/Admin/Ping — Contract Tests")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean AuthUseCase  auth;
    @MockBean AdminUseCase admin;

    private String body(String u, String p) {
        return "{\"username\":\"" + u + "\",\"password\":\"" + p + "\"}";
    }

    @Test @DisplayName("login 200 OK")
    void login200() throws Exception {
        when(auth.login(any())).thenReturn(LoginResult.ok(UUID.randomUUID(), "alice"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body("alice", "secret")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OK"))
            .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test @DisplayName("login 404 NOT_FOUND")
    void login404() throws Exception {
        when(auth.login(any())).thenReturn(LoginResult.notFound());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body("ghost", "x")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test @DisplayName("login 401 неверный пароль")
    void login401() throws Exception {
        when(auth.login(any())).thenReturn(LoginResult.invalid());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body("alice", "bad")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Неверный логин или пароль"));
    }

    @Test @DisplayName("login 423 LOCKED")
    void login423() throws Exception {
        when(auth.login(any())).thenReturn(LoginResult.locked());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body("alice", "x")))
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.status").value("LOCKED"));
    }

    @Test @DisplayName("login 200 RECOVERY")
    void loginRecovery() throws Exception {
        when(auth.login(any())).thenReturn(LoginResult.recovery("alice"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body("alice", "x")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RECOVERY"));
    }

    @Test @DisplayName("register 201")
    void register201() throws Exception {
        when(auth.register(any())).thenReturn(new AccountResult(UUID.randomUUID(), "bob"));
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body("bob", "pass")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("bob"));
    }

    @Test @DisplayName("register 409 если занят")
    void register409() throws Exception {
        when(auth.register(any())).thenThrow(new ContributorAlreadyExistsException("bob"));
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body("bob", "pass")))
            .andExpect(status().isConflict());
    }

    @Test @DisplayName("admin reset 200")
    void adminReset200() throws Exception {
        doNothing().when(admin).resetPassword(any());
        mockMvc.perform(post("/api/v1/admin/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"volkov\",\"password\":\"123123\",\"username\":\"friend\"}"))
            .andExpect(status().isOk());
    }

    @Test @DisplayName("admin reset 403 при неверных данных")
    void adminReset403() throws Exception {
        doThrow(new InvalidAdminCredentialsException()).when(admin).resetPassword(any());
        mockMvc.perform(post("/api/v1/admin/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"x\",\"password\":\"y\",\"username\":\"friend\"}"))
            .andExpect(status().isForbidden());
    }

    @Test @DisplayName("ping 200 OK")
    void ping() throws Exception {
        mockMvc.perform(get("/api/v1/ping"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OK"))
            .andExpect(jsonPath("$.timestamp").exists());
    }
}
