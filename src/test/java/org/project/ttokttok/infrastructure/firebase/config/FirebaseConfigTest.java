package org.project.ttokttok.infrastructure.firebase.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FirebaseConfigTest {

    @Autowired
    private FirebaseConfig firebaseConfig;

    @Test
    @DisplayName("파이어베이스 연결에 성공한다")
    void firebaseConnectionTest() {
        // given & when & then
        assertDoesNotThrow(() -> {
            FirebaseApp firebaseApp = firebaseConfig.firebaseApp();

            // Firebase 앱이 성공적으로 초기화되었는지 확인
            assertThat(firebaseApp).isNotNull();
            assertThat(firebaseApp.getName()).isEqualTo(FirebaseApp.DEFAULT_APP_NAME);
            assertThat(firebaseApp.getOptions()).isNotNull();
        });
    }

    @Test
    @DisplayName("파이어베이스 앱이 초기화되어 있다")
    void firebaseAppIsInitializedTest() {
        // given & when & then
        assertDoesNotThrow(() -> {
            firebaseConfig.firebaseApp();

            // FirebaseApp이 초기화되어 앱 목록에 포함되어 있는지 확인
            assertThat(FirebaseApp.getApps()).isNotEmpty();

            FirebaseApp defaultApp = FirebaseApp.getInstance();
            assertThat(defaultApp).isNotNull();
            assertThat(defaultApp.getName()).isEqualTo(FirebaseApp.DEFAULT_APP_NAME);
        });
    }

    @Test
    @DisplayName("Firebase Messaging 서비스에 접근할 수 있다")
    void firebaseMessagingAccessTest() {
        // given & when & then
        assertDoesNotThrow(() -> {
            FirebaseApp firebaseApp = firebaseConfig.firebaseApp();

            // Firebase Messaging 인스턴스를 가져올 수 있는지 확인
            FirebaseMessaging messaging = FirebaseMessaging.getInstance(firebaseApp);
            assertThat(messaging).isNotNull();
        });
    }

    @Test
    @DisplayName("중복 초기화 시에도 동일한 인스턴스를 반환한다")
    void firebaseAppSingletonTest() {
        // given & when & then
        assertDoesNotThrow(() -> {
            FirebaseApp firstApp = firebaseConfig.firebaseApp();
            FirebaseApp secondApp = firebaseConfig.firebaseApp();

            // 동일한 인스턴스를 반환하는지 확인
            assertThat(firstApp).isSameAs(secondApp);
            assertThat(FirebaseApp.getApps()).hasSize(1);
        });
    }

    @Test
    @DisplayName("Firebase Admin SDK가 정상적으로 작동한다")
    void firebaseAdminSdkOperationalTest() {
        // given & when & then
        assertDoesNotThrow(() -> {
            FirebaseApp firebaseApp = firebaseConfig.firebaseApp();

            // Firebase Admin SDK의 기본 기능들이 정상 작동하는지 확인
            assertThat(firebaseApp).isNotNull();
            assertThat(firebaseApp.getName()).isNotBlank();

            // Firebase Messaging 서비스 접근 확인
            FirebaseMessaging messaging = FirebaseMessaging.getInstance(firebaseApp);
            assertThat(messaging).isNotNull();

            // Firebase Options가 제대로 설정되었는지 확인
            assertThat(firebaseApp.getOptions()).isNotNull();
        });
    }
}