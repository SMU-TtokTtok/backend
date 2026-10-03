package org.project.ttokttok.domain.clubMember.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class ClubMemberAccessDeniedException extends CustomException {
    public ClubMemberAccessDeniedException() {
        super(ErrorMessage.MEMBER_ACCESS_DENIED);
    }
}
