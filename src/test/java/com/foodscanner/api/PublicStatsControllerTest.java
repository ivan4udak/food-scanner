package com.foodscanner.api;

import com.foodscanner.api.controller.GlobalExceptionHandler;
import com.foodscanner.api.controller.PublicStatsController;
import com.foodscanner.application.result.LeaderboardResult;
import com.foodscanner.application.result.PublicStatsResult;
import com.foodscanner.application.usecase.GetLeaderboardUseCase;
import com.foodscanner.application.usecase.GetPublicStatsUseCase;
import com.foodscanner.application.usecase.LeaderboardPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PublicStatsController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {com.foodscanner.infrastructure.config.WebConfig.class,
                   com.foodscanner.infrastructure.security.AuthInterceptor.class}))
@Import(GlobalExceptionHandler.class)
@DisplayName("PublicStatsController — Contract Tests")
class PublicStatsControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean GetPublicStatsUseCase getStats;
    @MockBean GetLeaderboardUseCase getLeaderboard;

    @Test
    @DisplayName("GET /public/stats — totals + today")
    void stats() throws Exception {
        when(getStats.execute()).thenReturn(new PublicStatsResult(
            new PublicStatsResult.Totals(12000, 3400, 15000, 180),
            new PublicStatsResult.Today(240, 80, 310)));

        mockMvc.perform(get("/api/v1/public/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totals.scans").value(12000))
            .andExpect(jsonPath("$.totals.catalogEntries").value(3400))
            .andExpect(jsonPath("$.totals.photos").value(15000))
            .andExpect(jsonPath("$.totals.contributors").value(180))
            .andExpect(jsonPath("$.today.scans").value(240));
    }

    @Test
    @DisplayName("GET /public/leaderboard — items с rank/score")
    void leaderboard() throws Exception {
        when(getLeaderboard.execute(eq(LeaderboardPeriod.WEEK), eq(5))).thenReturn(
            new LeaderboardResult("week", List.of(
                new LeaderboardResult.Entry(1, "ivan", 120, 340, 500, 120))));

        mockMvc.perform(get("/api/v1/public/leaderboard").param("period", "week").param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.period").value("week"))
            .andExpect(jsonPath("$.items[0].rank").value(1))
            .andExpect(jsonPath("$.items[0].username").value("ivan"))
            .andExpect(jsonPath("$.items[0].completedEntries").value(120))
            .andExpect(jsonPath("$.items[0].score").value(120));
    }
}
