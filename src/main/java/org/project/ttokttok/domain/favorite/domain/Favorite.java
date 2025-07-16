package org.project.ttokttok.domain.favorite.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.global.entity.BaseTimeEntity;

import java.util.UUID;

@Entity
@Getter
@Table(name = "user_favorites")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite extends BaseTimeEntity {

    @PrePersist
    private void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    @Id
    @Column(length = 36, updatable = false, unique = true)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false, updatable = false)
    private Club club;

    @Builder
    private Favorite(User user, Club club) {
        this.user = user;
        this.club = club;
    }
}
