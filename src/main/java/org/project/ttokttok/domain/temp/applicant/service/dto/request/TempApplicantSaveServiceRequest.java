package org.project.ttokttok.domain.temp.applicant.service.dto.request;

import java.util.List;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.temp.applicant.domain.TempAnswer;
import org.springframework.web.multipart.MultipartFile;

public record TempApplicantSaveServiceRequest(
        String email,
        String formId,
        Data data,
        List<String> questionIds,
        List<MultipartFile> files
) {
    public record Data(
            String name,
            Integer age,
            String major,
            String email,
            String phone,
            StudentStatus studentStatus,
            Grade grade,
            Gender gender,
            List<TempAnswer> answers
    ) {
    }
}
