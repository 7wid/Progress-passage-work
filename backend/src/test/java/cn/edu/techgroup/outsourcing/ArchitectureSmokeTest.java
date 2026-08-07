package cn.edu.techgroup.outsourcing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ArchitectureSmokeTest {

    @Test
    void applicationClassShouldExist() {
        assertThat(TechRequestApplication.class).isNotNull();
    }
}
