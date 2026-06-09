package com.foodscanner.domain.policy;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogQualityPolicyTest {

    @Test
    void emptyIsZero() {
        assertThat(CatalogQualityPolicy.score(Set.of())).isZero();
        assertThat(CatalogQualityPolicy.score(null)).isZero();
    }

    @Test
    void fullSetIs100() {
        assertThat(CatalogQualityPolicy.score(
            Set.of("BARCODE", "FRONT", "INGREDIENTS", "NUTRITION", "EXTRA"))).isEqualTo(100);
    }

    @Test
    void weightsPerType() {
        assertThat(CatalogQualityPolicy.score(Set.of("BARCODE"))).isEqualTo(20);
        assertThat(CatalogQualityPolicy.score(Set.of("FRONT"))).isEqualTo(20);
        assertThat(CatalogQualityPolicy.score(Set.of("INGREDIENTS", "NUTRITION"))).isEqualTo(50);
    }

    @Test
    void backOrExtraCountsOnce() {
        assertThat(CatalogQualityPolicy.score(Set.of("BACK"))).isEqualTo(10);
        assertThat(CatalogQualityPolicy.score(Set.of("EXTRA"))).isEqualTo(10);
        assertThat(CatalogQualityPolicy.score(Set.of("BACK", "EXTRA"))).isEqualTo(10);
    }
}
