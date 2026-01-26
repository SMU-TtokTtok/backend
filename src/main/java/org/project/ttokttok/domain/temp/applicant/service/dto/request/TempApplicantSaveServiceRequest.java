package org.project.ttokttok.domain.temp.applicant.service.dto.request;

import java.util.List;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.temp.applicant.controller.dto.request.TempAnswer;
import org.project.ttokttok.domain.temp.applicant.controller.dto.request.TempApplicantSaveRequest;
import org.springframework.web.multipart.MultipartFile;

public record TempApplicantSaveServiceRequest(
        String email,
        String formId,
        Data data,
        List<String> questionIds,
        List<MultipartFile> files
) {
    public static TempApplicantSaveServiceRequest of(
            String email,
            String formId,
            TempApplicantSaveRequest request,
            List<String> questionIds,
            List<MultipartFile> files
    ) {
        return new TempApplicantSaveServiceRequest(
                email,
                formId,
                toData(request),
                questionIds,
                files
        );
    }

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

    private static Data toData(TempApplicantSaveRequest request) {
        return new Data(
                request.name(),
                request.age(),
                request.major(),
                request.email(),
                request.phone(),
                request.studentStatus(),
                request.grade(),
                request.gender(),
                request.answers()
        );
    }
}
