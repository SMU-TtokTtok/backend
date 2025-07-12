package org.project.ttokttok.domain.memo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.Applicant;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Memo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, unique = true)
    private String id;

    @Column(nullable = false, length = 100)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Applicant applicant;

    public Memo(String content) {
        this.content = content;
    }

    public void updateContent(final String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("메모 내용은 비워둘 수 없습니다.");
        }

        this.content = content;
    }
}


