package org.project.ttokttok.domain.clubMember.service.policy;

import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.springframework.stereotype.Component;

@Component
public class ClubAccessPolicy {
    public void validateAdmin(Club club, String username) {
        if (!club.isManagedBy(username)) // join
            throw new NotClubAdminException();
    }
}
