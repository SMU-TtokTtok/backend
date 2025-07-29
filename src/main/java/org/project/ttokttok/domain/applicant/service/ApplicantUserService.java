package org.project.ttokttok.domain.applicant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.applicant.controller.dto.request.ApplyFormRequest;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicantUserService {

    private final ApplicantRepository applicantRepository;
    private final ApplyFormRepository applyFormRepository;

    public void apply(String email, ApplyFormRequest request, Map<String, MultipartFile> files) {

    }
}
