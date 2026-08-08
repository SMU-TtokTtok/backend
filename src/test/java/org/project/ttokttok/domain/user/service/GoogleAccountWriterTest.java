package org.project.ttokttok.domain.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.exception.GoogleLinkTargetNotFoundException;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GoogleAccountWriterTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GoogleAccountWriter googleAccountWriter;

    private static final String SUB = "google-sub-123";
    private static final String SMU_EMAIL = "202021000@sangmyung.kr";

    private User localUser() {
        return User.signUp(SMU_EMAIL, "encoded-password", "김철수", true);
    }

    @Test
    @DisplayName("autoLink(): 연동 대상 계정이 사라졌으면 500이 아니라 409로 응답한다.")
    void autoLink_targetMissing_throwsConflict() {
        given(userRepository.findByEmail(SMU_EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> googleAccountWriter.autoLink(SMU_EMAIL, SUB))
                .isInstanceOf(GoogleLinkTargetNotFoundException.class)
                .extracting(e -> ((GoogleLinkTargetNotFoundException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("autoLink(): 연동 대상이 없을 때 예외 메시지에 이메일이 노출되지 않는다.")
    void autoLink_targetMissing_doesNotLeakEmail() {
        given(userRepository.findByEmail(SMU_EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> googleAccountWriter.autoLink(SMU_EMAIL, SUB))
                .isInstanceOf(GoogleLinkTargetNotFoundException.class)
                .hasMessageNotContaining(SMU_EMAIL);
    }

    @Test
    @DisplayName("autoLink(): 연동 대상이 있으면 구글 계정을 연동해 저장한다.")
    void autoLink_success() {
        User user = localUser();
        given(userRepository.findByEmail(SMU_EMAIL)).willReturn(Optional.of(user));
        given(userRepository.saveAndFlush(user)).willReturn(user);

        User linked = googleAccountWriter.autoLink(SMU_EMAIL, SUB);

        assertThat(linked.getProviderId()).isEqualTo(SUB);
    }
}
