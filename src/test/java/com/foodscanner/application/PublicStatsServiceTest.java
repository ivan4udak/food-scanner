package com.foodscanner.application;

import com.foodscanner.application.port.StatsReadPort;
import com.foodscanner.application.result.LeaderboardResult;
import com.foodscanner.application.result.LeaderboardRow;
import com.foodscanner.application.service.PublicStatsService;
import com.foodscanner.application.usecase.LeaderboardPeriod;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class PublicStatsServiceTest {

    private final StatsReadPort port = mock(StatsReadPort.class);
    private final PublicStatsService service = new PublicStatsService(port);

    @Test
    void assignsRankAndScoreFromCompletedEntries() {
        when(port.leaderboard(any(), anyInt())).thenReturn(List.of(
            new LeaderboardRow("alice", 12, 30, 50),
            new LeaderboardRow("bob", 5, 80, 10)));

        LeaderboardResult result = service.execute(LeaderboardPeriod.ALL, 10);

        assertThat(result.period()).isEqualTo("all");
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).rank()).isEqualTo(1);
        assertThat(result.items().get(0).username()).isEqualTo("alice");
        assertThat(result.items().get(0).score()).isEqualTo(12);   // score = completedEntries
        assertThat(result.items().get(1).rank()).isEqualTo(2);
        assertThat(result.items().get(1).score()).isEqualTo(5);
    }

    @Test
    void clampsLimit() {
        when(port.leaderboard(any(), anyInt())).thenReturn(List.of());
        ArgumentCaptor<Integer> limit = ArgumentCaptor.forClass(Integer.class);

        service.execute(LeaderboardPeriod.WEEK, 500);
        service.execute(LeaderboardPeriod.WEEK, 0);

        verify(port, times(2)).leaderboard(any(), limit.capture());
        assertThat(limit.getAllValues().get(0)).isEqualTo(100); // 500 → max 100
        assertThat(limit.getAllValues().get(1)).isEqualTo(10);  // 0 → default 10
    }

    @Test
    void allPeriodPassesNullSince() {
        when(port.leaderboard(any(), anyInt())).thenReturn(List.of());
        service.execute(LeaderboardPeriod.ALL, 10);
        verify(port).leaderboard(isNull(), eq(10));
    }

    @Test
    void weekPeriodPassesNonNullSince() {
        when(port.leaderboard(any(), anyInt())).thenReturn(List.of());
        service.execute(LeaderboardPeriod.WEEK, 10);
        verify(port).leaderboard(notNull(), eq(10));
    }
}
