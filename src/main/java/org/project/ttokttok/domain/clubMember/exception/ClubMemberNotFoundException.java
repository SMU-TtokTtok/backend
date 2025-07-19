package org.project.ttokttok.domain.clubMember.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class ClubMemberNotFoundException extends CustomException {
    public ClubMemberNotFoundException() {
        super(ErrorMessage.MEMBER_NOT_FOUND);
    }
}
