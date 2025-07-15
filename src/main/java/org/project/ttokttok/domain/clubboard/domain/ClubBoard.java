package org.project.ttokttok.domain.clubboard.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.global.entity.BaseTimeEntity;

import java.util.UUID;

@Entity
@Getter
@Table(name = "club_boards")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubBoard extends BaseTimeEntity {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false, updatable = false)
    private Club club;

    @Builder
    private ClubBoard(String title, String content, Club club) {
        this.title = title;
        this.content = content;
        this.club = club;
    }

    // ------- 정적 메서드 -------
    public static ClubBoard create(String title, String content, Club club) {
        validateTitle(title);
        validateContent(content);
        validateClub(club);

        return ClubBoard.builder()
                .title(title)
                .content(content)
                .club(club)
                .build();
    }

    // ------- 유효성 검사 메서드 -------
    private static void validateClub(Club club) {
        if (club == null) {
            throw new IllegalArgumentException("Club cannot be null.");
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content cannot be null or blank.");
        }
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or blank.");
        }
    }
}
