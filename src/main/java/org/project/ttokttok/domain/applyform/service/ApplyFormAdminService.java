package org.project.ttokttok.domain.applyform.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplyFormAdminService {

    private final ApplyFormRepository applyFormRepository;

    // 지원 폼 생성 메서드
    public void createApplyForm(ApplyFormCreateServiceRequest request) {

    }
}
