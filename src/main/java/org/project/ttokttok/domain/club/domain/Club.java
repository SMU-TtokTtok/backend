package org.project.ttokttok.domain.club.domain;

import jakarta.persistence.*;
import lombok.*;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.global.entity.BaseTimeEntity;

import java.util.UUID;

@Entity
@Getter
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

    @Setter
    @Column(length = 50, nullable = false, unique = true)
    private String name;

    @Setter
    @Column(name = "profile_img")
    private String profileImageUrl;

    @Setter
    @Column(nullable = false)
    private String summary; // 한줄 소개

    @Setter
    @Enumerated(EnumType.STRING)
    private ClubType clubType; // 동아리 유형

    @Setter
    @Enumerated(EnumType.STRING)
    private ClubCategory clubCategory; // 동아리 유형

    @Setter
    @Column(length = 30, nullable = false)
    private String customCategory;

    @Setter
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Setter
    @Column(nullable = false)
    private boolean recruiting; // 모집여부

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    // todo: 추후에 안내 메시지 등으로 변경 필요.
    @Builder
    private Club(Admin admin) {
        this.admin = admin;
        this.name = "동아리 이름";
        this.profileImageUrl = null;
        this.summary = "동아리 한줄 소개를 적어주세요.";
        this.clubType = null;
        this.clubCategory = null;
        this.customCategory = "";
        this.content = "동아리 소개를 적어주세요.";
        this.recruiting = false;
    }
}
