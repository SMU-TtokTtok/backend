package org.project.ttokttok.domain.applyform.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.applyform.service.dto.request.ApplyFormCreateServiceRequest;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplyFormAdminService {

    private final ApplyFormRepository applyFormRepository;
    private final ClubRepository clubRepository;

    // 지원 폼 생성 메서드
    public String createApplyForm(ApplyFormCreateServiceRequest request) {
        Club club = clubRepository.findById(request.clubId())
                .orElseThrow(ClubNotFoundException::new);

        // 관리자 권한 검증
        validateAdmin(club.getAdmin().getUsername(), request.username());

        // 숫자 입력으로 들어온 set을 ApplicableGrade로 변환
        Set<ApplicableGrade> applicableGrades = request.applicableGrades()
                .stream()
                .map(ApplicableGrade::from)
                .collect(Collectors.toSet());

        // 지원 폼 생성
        ApplyForm applyForm = ApplyForm.createApplyForm(
                club,
                request.hasInterview(),
                request.recruitStartDate(),
                request.recruitEndDate(),
                request.interviewStartDate().orElse(null),
                request.interviewEndDate().orElse(null),
                request.maxApplyCount(),
                applicableGrades,
                request.title(),
                request.subTitle(),
                request.applyForm()
        );

        return applyFormRepository.save(applyForm)
                .getId();
    }

    private void validateAdmin(String adminName, String requestAdminName) {
        if (!adminName.equals(requestAdminName)) {
            throw new NotClubAdminException();
        }
    }
}
