package org.project.ttokttok.domain.clubMember.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.global.entity.BaseTimeEntity;

@Entity
@Getter
@Table(name = "club_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, unique = true)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grade grade;

    @Column(nullable = false)
    private String major;

    @Builder
    private ClubMember(Club club, User user, MemberRole role, Grade grade, String major) {
        this.club = club;
        this.user = user;
        this.role = role != null ? role : MemberRole.MEMBER;
        this.grade = grade != null ? grade : Grade.FIRST_GRADE; // 기본값은 1학년
        this.major = major != null ? major : "N/A"; // 학과 값 에러 시
    }

    public static ClubMember create(Club club,
                                    User user,
                                    MemberRole role,
                                    Grade grade,
                                    String major) {

        return ClubMember.builder()
                .club(club)
                .user(user)
                .role(role)
                .grade(grade)
                .major(major)
                .build();
    }

    public void changeRole(MemberRole newRole) {
        this.role = newRole;
    }

//    public boolean isPresident() {
//        return this.role == MemberRole.PRESIDENT;
//    }
//
//    public boolean isVicePresident() {
//        return this.role == MemberRole.VICE_PRESIDENT;
//    }
//
//    public boolean isExecutive() {
//        return this.role == MemberRole.EXECUTIVE;
//    }
}
