package org.project.ttokttok.domain.clubMember.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class AlreadyClubMemberException extends CustomException {
    public AlreadyClubMemberException() {
        super(ErrorMessage.ALREADY_CLUB_MEMBER);
    }
}
