package org.project.ttokttok.domain.club.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.project.ttokttok.global.entity.BaseTimeEntity;

import java.util.UUID;

@Entity
@Table(name = "clubs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club extends BaseTimeEntity {

    //UUID 생성 전략
    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    @Id
    @Column(length = 36, updatable = false, unique = true)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String profileImageUrl;

    private String summary; // 한줄 소개

    @Enumerated(EnumType.STRING)
    private ClubType type; // 동아리 유형

    @Enumerated(EnumType.STRING)
    private ClubCategory category; // 동아리 유형

    @Column(length = 30)
    private String customCategory;

    @Column(columnDefinition = "TEXT")
    private String content;

    private boolean recruiting; // 모집여부
}
