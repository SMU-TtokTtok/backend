package org.project.ttokttok.global.error;

import org.project.ttokttok.global.error.dto.ErrorResponse;
import org.project.ttokttok.global.error.dto.ValidErrorDetails;
import org.project.ttokttok.global.error.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({CustomException.class})
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        ErrorResponse response = ErrorResponse.builder()
                .statusCode(e.getStatus().value())
                .details(e.getMessage())
                .build();

        return ResponseEntity.status(e.getStatus())
                .body(response);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createValidErrorResponse(e));
    }

    private ErrorResponse createValidErrorResponse(MethodArgumentNotValidException e) {
        List<ValidErrorDetails> errors = getValidErrorDetails(e);

        return ErrorResponse.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .details(errors)
                .build();
    }

    private List<ValidErrorDetails> getValidErrorDetails(MethodArgumentNotValidException e) {
        return e.getBindingResult().getFieldErrors()
                .stream()
                .map(error ->
                        new ValidErrorDetails(
                                error.getField(),
                                error.getDefaultMessage())
                )
                .toList();
    }

}
