package org.project.ttokttok.infrastructure.firebase.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Configuration
@RequiredArgsConstructor
public class FirebaseConfig {

    private final ResourceLoader resourceLoader;

    /**
     * Firebase 서비스 계정 키의 위치. Spring 리소스 표기(classpath:, file:)를 따른다.
     *
     * <p>기본값은 기존 동작 그대로 {@code firebase.json} 이 가리키는 classpath 리소스다.
     * 컨테이너 운영 환경에서는 이 키를 이미지에 굽지 않고 런타임에 마운트하므로
     * {@code file:/app/config/...} 형태로 덮어쓴다. 서비스 계정 키가 이미지 레이어에
     * 남으면 이미지를 얻은 쪽이 그대로 인증에 쓸 수 있어, 마운트로 분리한다.
     */
    @Value("${firebase.credentials-location:classpath:${firebase.json}}")
    private String credentialsLocation;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        Resource resource = resourceLoader.getResource(credentialsLocation);

        try (InputStream serviceAccount = resource.getInputStream()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.initializeApp(options);
            }

            return FirebaseApp.getInstance();
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
