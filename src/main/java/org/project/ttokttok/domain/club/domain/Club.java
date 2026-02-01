package org.project.ttokttok.domain.club.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.club.domain.enums.ClubCategory;
import org.project.ttokttok.domain.club.domain.enums.ClubType;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.club.service.dto.request.ClubContentUpdateServiceRequest;
import org.project.ttokttok.domain.club.service.dto.request.ClubPatchRequest;
import org.project.ttokttok.domain.clubMember.domain.ClubMember;
import org.project.ttokttok.global.entity.BaseTimeEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Entity
@Getter
@Table(name = "clubs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, unique = true)
    private String id;

    @Column(length = 50, nullable = false, unique = true)
    private String name;

    @Column(name = "profile_img")
    private String profileImageUrl;

    @Column(nullable = false)
    private String summary; // 한줄 소개

    @Enumerated(EnumType.STRING)
    private ClubType clubType; // 동아리 유형

    @Enumerated(EnumType.STRING)
    private ClubCategory clubCategory; // 동아리 유형

    @Enumerated(EnumType.STRING)
    private ClubUniv clubUniv; // 대학 구분

    @Column(length = 30, nullable = false)
    private String customCategory;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0L;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ClubMember> clubMembers = new ArrayList<>();

    @Builder
    private Club(Admin admin, String clubName, ClubUniv clubUniv) {
        this.admin = admin;
        this.name = clubName;
        this.profileImageUrl = null;
        this.summary = "아직 동아리 소개가 없어요 🙂";
        this.clubType = ClubType.CENTRAL;
        this.clubCategory = ClubCategory.ACADEMIC;
        this.clubUniv = clubUniv;
        this.customCategory = "";
        this.content = "동아리 소개가 아직 작성되지 않았어요!";
    }

    public void updateProfileImgUrl(String imageUrl) {
        this.profileImageUrl = imageUrl;
    }

    //PATCH 용 메서드
    public void updateFrom(ClubPatchRequest req) {
        updateField(req.name(), v -> this.name = v);
        updateField(req.clubType(), v -> this.clubType = v);
        updateField(req.clubCategory(), v -> this.clubCategory = v);
        updateField(req.clubUniv(), v -> this.clubUniv = v);
        updateField(req.customCategory(), v -> this.customCategory = v);
        updateField(req.summary(), v -> this.summary = v);
        updateField(req.content(), v -> this.content = v);
    }

    private <T> void updateField(JsonNullable<T> nullable, Consumer<T> setter) {
        if (nullable != null && nullable.isPresent()) {
            setter.accept(nullable.get());
        }
    }

    public void updateViewCount() {
        viewCount += 1;
    }
}
