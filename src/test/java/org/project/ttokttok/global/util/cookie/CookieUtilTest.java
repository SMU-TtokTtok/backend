package org.project.ttokttok.global.util.cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CookieUtil - 쿠키 생성/만료")
class CookieUtilTest {

    private final CookieUtil cookieUtil = createCookieUtil("Lax", false);

    private CookieUtil createCookieUtil(String sameSite, boolean secure) {
        CookieUtil util = new CookieUtil();
        ReflectionTestUtils.setField(util, "cookieSameSite", sameSite);
        ReflectionTestUtils.setField(util, "cookieSecure", secure);
        ReflectionTestUtils.setField(util, "activeProfile", "test");
        return util;
    }

    @AfterEach
    void clearProfileProperty() {
        System.clearProperty("spring.profiles.active");
    }

    @Test
    @DisplayName("createResponseCookie는 httpOnly/path/maxAge가 설정된 쿠키를 생성한다")
    void createResponseCookie() {
        ResponseCookie cookie = cookieUtil.createResponseCookie("access", "token-value", Duration.ofMinutes(30));

        assertThat(cookie.getName()).isEqualTo("access");
        assertThat(cookie.getValue()).isEqualTo("token-value");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMinutes(30));
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
    }

    @Test
    @DisplayName("expireResponseCookie는 값이 비어있고 maxAge가 0인 쿠키를 생성한다")
    void expireResponseCookie() {
        ResponseCookie cookie = cookieUtil.expireResponseCookie("access");

        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    @DisplayName("expireBothTokenCookies는 액세스/리프레시 쿠키 2개를 만료시킨다")
    void expireBothTokenCookies() {
        ResponseCookie[] cookies = cookieUtil.expireBothTokenCookies();

        assertThat(cookies).hasSize(2);
        assertThat(cookies[0].getMaxAge()).isEqualTo(Duration.ZERO);
        assertThat(cookies[1].getMaxAge()).isEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("expireUserTokenCookies는 사용자용 쿠키 2개를 만료시킨다")
    void expireUserTokenCookies() {
        ResponseCookie[] cookies = cookieUtil.expireUserTokenCookies();

        assertThat(cookies).hasSize(2);
        assertThat(cookies[0].getValue()).isEmpty();
        assertThat(cookies[1].getValue()).isEmpty();
    }

    @Test
    @DisplayName("secure=true로 설정하면 생성 쿠키의 secure 플래그가 활성화된다")
    void createResponseCookie_withSecure() {
        CookieUtil secureUtil = createCookieUtil("None", true);

        ResponseCookie cookie = secureUtil.createResponseCookie("access", "v", Duration.ofMinutes(1));

        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("None");
    }

    @Test
    @DisplayName("static 메서드는 prod 프로필에서 secure=true, SameSite=None을 사용한다")
    void createResponseCookieStatic_prod() {
        System.setProperty("spring.profiles.active", "prod");

        ResponseCookie cookie = CookieUtil.createResponseCookieStatic("access", "v", Duration.ofMinutes(1));

        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("None");
    }

    @Test
    @DisplayName("static 메서드는 dev 프로필에서 secure=false, SameSite=Lax를 사용한다")
    void createResponseCookieStatic_dev() {
        System.setProperty("spring.profiles.active", "dev");

        ResponseCookie cookie = CookieUtil.createResponseCookieStatic("access", "v", Duration.ofMinutes(1));

        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
    }

    @Test
    @DisplayName("static expire 메서드는 만료 쿠키를 생성한다")
    void expireResponseCookieStatic() {
        System.setProperty("spring.profiles.active", "dev");

        ResponseCookie cookie = CookieUtil.expireResponseCookieStatic("access");

        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
    }
}
