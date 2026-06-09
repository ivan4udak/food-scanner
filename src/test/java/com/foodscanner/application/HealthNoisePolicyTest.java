package com.foodscanner.application;

import com.foodscanner.application.service.HealthNoisePolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthNoisePolicyTest {

    private final HealthNoisePolicy policy = new HealthNoisePolicy();

    @Test
    void successfulPingIsNoise() {
        assertThat(policy.isNoise("/api/v1/ping", 200, "INFO")).isTrue();
        assertThat(policy.isNoise("/api/v1/health", 200, "INFO")).isTrue();
    }

    @Test
    void routinePingWithoutStatusIsNoise() {
        assertThat(policy.isNoise("/api/v1/ping", null, "INFO")).isTrue();
    }

    @Test
    void failedPingIsNotNoise() {
        assertThat(policy.isNoise("/api/v1/ping", 503, "ERROR")).isFalse();
        assertThat(policy.isNoise("/api/v1/health", 500, "WARN")).isFalse();
    }

    @Test
    void networkErrorPingWithoutStatusIsNotNoise() {
        assertThat(policy.isNoise("/api/v1/ping", null, "ERROR")).isFalse();
        assertThat(policy.isNoise("/api/v1/health", null, "WARN")).isFalse();
    }

    @Test
    void nonHealthPathIsNeverNoise() {
        assertThat(policy.isNoise("/api/v1/scan", 200, "INFO")).isFalse();
        assertThat(policy.isNoise(null, 200, "INFO")).isFalse();
    }
}
