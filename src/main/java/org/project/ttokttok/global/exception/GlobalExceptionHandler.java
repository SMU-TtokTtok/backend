package org.project.ttokttok.global.exception;

import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.project.ttokttok.global.exception.dto.ValidErrorDetails;
import org.project.ttokttok.global.exception.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // 들어오는 예외들을 캐치하여 처리하는 클래스
    // 메서드에 달리는 @Valid 어노테이션이나
    // 커스텀 예외 발생 시 응답 핸들링에 자동 적용된다.

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

    @ExceptionHandler({Exception.class})
    public ResponseEntity<ErrorResponse> handleUncaughtException(Exception e) {
        ErrorResponse response = ErrorResponse.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .details("서버 내부 오류가 발생했습니다.")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorResponse response = ErrorResponse.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .details(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
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
