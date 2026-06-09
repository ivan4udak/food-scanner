package com.foodscanner.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContributorRoleTest {

    @Test
    void newContributorIsUserByDefault() {
        Contributor c = Contributor.createWithCredentials("alice", "hash");
        assertThat(c.getRole()).isEqualTo(ContributorRole.USER);
        assertThat(c.isAdmin()).isFalse();
    }

    @Test
    void assignRoleMakesAdmin() {
        Contributor c = Contributor.createWithCredentials("alice", "hash");
        c.assignRole(ContributorRole.ADMIN);
        assertThat(c.isAdmin()).isTrue();
    }

    @Test
    void parseIsLenient() {
        assertThat(ContributorRole.parse("admin")).isEqualTo(ContributorRole.ADMIN);
        assertThat(ContributorRole.parse(null)).isEqualTo(ContributorRole.USER);
        assertThat(ContributorRole.parse("garbage")).isEqualTo(ContributorRole.USER);
    }

    @Test
    void superAdminAndAdminAreAdmin() {
        assertThat(ContributorRole.ADMIN.isAdmin()).isTrue();
        assertThat(ContributorRole.SUPER_ADMIN.isAdmin()).isTrue();
        assertThat(ContributorRole.USER.isAdmin()).isFalse();
    }
}
