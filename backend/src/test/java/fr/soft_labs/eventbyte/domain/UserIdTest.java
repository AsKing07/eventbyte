package fr.soft_labs.eventbyte.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdTest {

    @Test
    void garde_le_sub_tel_quel() {
        assertThat(new UserId("f3a1-sub").value()).isEqualTo("f3a1-sub");
    }

    @Test
    void refuse_un_sub_vide() {
        assertThatThrownBy(() -> new UserId(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuse_un_sub_absent() {
        assertThatThrownBy(() -> new UserId(null)).isInstanceOf(NullPointerException.class);
    }
}
