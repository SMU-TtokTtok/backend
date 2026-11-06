package org.project.ttokttok.domain.clubmember.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.CustomException;

public class ClubMemberAccessDeniedException extends CustomException {
    public ClubMemberAccessDeniedException() {
        super(ErrorMessage.MEMBER_ACCESS_DENIED);
    }
}
