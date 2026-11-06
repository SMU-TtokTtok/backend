package org.project.ttokttok.global.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogMaskerTest {

    @Test
    void masksLocalPartAndKeepsDomain() {
        assertThat(LogMasker.email("kim@sangmyung.kr")).isEqualTo("k***@sangmyung.kr");
    }

    @Test
    void masksValuesThatAreNotEmails() {
        assertThat(LogMasker.email(null)).isEqualTo("***");
        assertThat(LogMasker.email("plain-text")).isEqualTo("***");
        assertThat(LogMasker.email("@sangmyung.kr")).isEqualTo("***");
    }
}
