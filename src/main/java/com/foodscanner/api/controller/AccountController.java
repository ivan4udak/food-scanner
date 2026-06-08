package com.foodscanner.api.controller;

import com.foodscanner.api.dto.stats.LeaderboardVisibilityRequest;
import com.foodscanner.api.dto.stats.LeaderboardVisibilityResponse;
import com.foodscanner.application.usecase.SetLeaderboardVisibilityUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Слой: api.
 *
 * Действия текущего пользователя (Bearer):
 *   POST /api/v1/me/leaderboard-visibility — скрыть/показать себя в публичном рейтинге.
 */
@RestController
@RequestMapping("/api/v1/me")
public class AccountController {

    private static final String AUTH_CONTRIBUTOR = "authContributorId";

    private final SetLeaderboardVisibilityUseCase setVisibility;

    public AccountController(SetLeaderboardVisibilityUseCase setVisibility) {
        this.setVisibility = setVisibility;
    }

    @PostMapping("/leaderboard-visibility")
    public ResponseEntity<LeaderboardVisibilityResponse> setVisibility(
            @Valid @RequestBody LeaderboardVisibilityRequest request,
            @RequestAttribute(AUTH_CONTRIBUTOR) UUID contributorId) {
        boolean hidden = setVisibility.execute(contributorId, Boolean.TRUE.equals(request.hidden()));
        return ResponseEntity.ok(new LeaderboardVisibilityResponse(hidden));
    }
}
