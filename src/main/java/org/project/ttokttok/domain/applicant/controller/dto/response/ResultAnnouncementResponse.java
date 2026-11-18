package org.project.ttokttok.domain.applicant.controller.dto.response;

import org.project.ttokttok.domain.applicant.service.dto.response.ResultAnnouncementServiceResponse;

public record ResultAnnouncementResponse(
        String announcementId,
        int passMailCount,
        int failMailCount
) {
    public static ResultAnnouncementResponse from(ResultAnnouncementServiceResponse response) {
        return new ResultAnnouncementResponse(
                response.announcementId(),
                response.passMailCount(),
                response.failMailCount()
        );
    }
}
