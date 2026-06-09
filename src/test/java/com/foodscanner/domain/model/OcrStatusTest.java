package com.foodscanner.domain.model;

import com.foodscanner.domain.model.ocr.OcrStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OcrStatusTest {

    @Test
    void codesAre0to5() {
        assertThat(OcrStatus.QUEUED.code()).isZero();
        assertThat(OcrStatus.SUCCESS.code()).isEqualTo(4);
        assertThat(OcrStatus.ERROR.code()).isEqualTo(5);
    }

    @Test
    void fromCodeRoundTrip() {
        for (OcrStatus s : OcrStatus.values()) {
            assertThat(OcrStatus.fromCode(s.code())).isEqualTo(s);
        }
        assertThatThrownBy(() -> OcrStatus.fromCode(9)).isInstanceOf(IllegalArgumentException.class);
    }
}
