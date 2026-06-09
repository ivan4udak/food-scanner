package com.foodscanner.api;

import com.foodscanner.api.controller.AdminRoleController;
import com.foodscanner.api.controller.GlobalExceptionHandler;
import com.foodscanner.application.usecase.SetUserRoleUseCase;
import com.foodscanner.domain.exception.AccessDeniedException;
import com.foodscanner.domain.model.ContributorRole;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminRoleController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {com.foodscanner.infrastructure.config.WebConfig.class,
                   com.foodscanner.infrastructure.security.AuthInterceptor.class,
                   com.foodscanner.infrastructure.security.AdminGuardInterceptor.class}))
@Import(GlobalExceptionHandler.class)
@DisplayName("AdminRoleController — Contract Tests")
class AdminRoleControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean SetUserRoleUseCase setUserRole;

    @Test
    @DisplayName("SUPER_ADMIN меняет роль → 200")
    void superAdminSetsRole() throws Exception {
        UUID id = UUID.randomUUID();
        when(setUserRole.execute(eq(ContributorRole.SUPER_ADMIN), eq(id), eq(ContributorRole.ADMIN)))
            .thenReturn(ContributorRole.ADMIN);

        mockMvc.perform(post("/api/v1/admin/users/" + id + "/role")
                .requestAttr("authRole", "SUPER_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("ADMIN (не супер) → 403")
    void adminForbidden() throws Exception {
        when(setUserRole.execute(any(), any(), any())).thenThrow(new AccessDeniedException("нельзя"));

        mockMvc.perform(post("/api/v1/admin/users/" + UUID.randomUUID() + "/role")
                .requestAttr("authRole", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isForbidden());
    }
}
