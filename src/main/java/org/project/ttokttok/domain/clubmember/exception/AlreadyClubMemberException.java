package org.project.ttokttok.domain.clubmember.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.CustomException;

public class AlreadyClubMemberException extends CustomException {
    public AlreadyClubMemberException() {
        super(ErrorMessage.ALREADY_CLUB_MEMBER);
    }
}
