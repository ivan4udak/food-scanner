package com.foodscanner.application;

import com.foodscanner.application.service.TelemetrySanitizer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TelemetrySanitizerTest {

    private final TelemetrySanitizer sanitizer = new TelemetrySanitizer();

    @Test
    void masksBearerAndJwtInStrings() {
        assertThat(sanitizer.maskString("hdr Bearer eyJabc.def.ghijklmno end"))
            .contains("Bearer ********")
            .doesNotContain("eyJabc.def.ghijklmno");
    }

    @Test
    void masksSecretKeysInMap() {
        Map<String, Object> in = Map.of(
            "password", "p",
            "accessToken", "a",
            "authorization", "Bearer xyz",
            "cookie", "sid=1",
            "ok", "fine");
        Map<String, Object> out = sanitizer.maskMap(in);
        assertThat(out.get("password")).isEqualTo("********");
        assertThat(out.get("accessToken")).isEqualTo("********");
        assertThat(out.get("authorization")).isEqualTo("********");
        assertThat(out.get("cookie")).isEqualTo("********");
        assertThat(out.get("ok")).isEqualTo("fine");
    }

    @Test
    void masksNestedMapsAndLists() {
        Map<String, Object> in = Map.of(
            "headers", Map.of("Authorization", "Bearer secrettoken1234567"),
            "items", List.of(Map.of("refreshToken", "r")));
        Map<String, Object> out = sanitizer.maskMap(in);
        String s = out.toString();
        assertThat(s).doesNotContain("secrettoken1234567");
        assertThat(s).doesNotContain("=r");
    }

    @Test
    void nullSafe() {
        assertThat(sanitizer.maskString(null)).isNull();
        assertThat(sanitizer.maskMap(null)).isNull();
    }
}
