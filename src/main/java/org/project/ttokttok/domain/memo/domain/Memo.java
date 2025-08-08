package org.project.ttokttok.domain.memo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.DocumentPhase;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Memo {

    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, unique = true)
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, length = 100)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_phase_id", nullable = false)
    private DocumentPhase documentPhase;

    @Builder
    private Memo(DocumentPhase documentPhase, String content) {
        this.documentPhase = documentPhase;
        this.content = content;
    }

    public static Memo create(DocumentPhase documentPhase, String content) {
        return Memo.builder()
                .documentPhase(documentPhase)
                .content(content)
                .build();
    }

    public void updateContent(final String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("메모 내용은 비워둘 수 없습니다.");
        }

        this.content = content;
    }
}


