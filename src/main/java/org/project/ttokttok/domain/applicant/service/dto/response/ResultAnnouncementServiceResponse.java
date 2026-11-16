package org.project.ttokttok.domain.applicant.service.dto.response;

import org.project.ttokttok.domain.applicant.domain.ResultAnnouncement;

public record ResultAnnouncementServiceResponse(
        String announcementId,
        int passMailCount,
        int failMailCount
) {
    public static ResultAnnouncementServiceResponse from(ResultAnnouncement announcement) {
        return new ResultAnnouncementServiceResponse(
                announcement.getId(),
                announcement.getPassMailCount(),
                announcement.getFailMailCount()
        );
    }
}
