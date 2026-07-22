package org.project.ttokttok.domain.notice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ttokttok.global.entity.BaseTimeEntity;

import java.util.UUID;

@Entity
@Getter
@Table(name = "notices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseTimeEntity {

    @PrePersist
    private void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    @Id
    @Column(length = 36, updatable = false, unique = true)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 작성한 운영자 username (감사/책임 추적용)
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(nullable = false)
    private int viewCount;

    @Builder
    private Notice(String title, String content, String createdBy) {
        this.title = title;
        this.content = content;
        this.createdBy = createdBy;
        this.viewCount = 0;
    }

    // ------- 정적 메서드 -------
    public static Notice create(String title, String content, String createdBy) {
        validateTitle(title);
        validateContent(content);

        return Notice.builder()
                .title(title)
                .content(content)
                .createdBy(createdBy)
                .build();
    }

    // ------- 유효성 검사 메서드 -------
    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or blank.");
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content cannot be null or blank.");
        }
    }
}
