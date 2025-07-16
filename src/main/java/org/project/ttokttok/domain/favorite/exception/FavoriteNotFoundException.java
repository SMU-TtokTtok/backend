package org.project.ttokttok.domain.favorite.exception;

import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;

public class FavoriteNotFoundException extends CustomException {
    public FavoriteNotFoundException() {
        super(ErrorMessage.FAVORITE_NOT_FOUND);
    }
} 