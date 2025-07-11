package org.project.ttokttok.global.exception;

import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.project.ttokttok.global.exception.dto.ValidErrorDetails;
import org.project.ttokttok.global.exception.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "경로에 잘못된 값이 입력되었습니다.";

        // 열거형 타입 변환 오류인 경우 더 구체적인 메시지 제공
        if (e.getRequiredType() != null && e.getRequiredType().isEnum()) {
            message = String.format("'%s'에 유효하지 않은 값이 입력되었습니다. 허용된 값: %s",
                    e.getName(),
                    String.join(", ", getEnumValues(e.getRequiredType())));
        }

        ErrorResponse response = ErrorResponse.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .details(message)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // 허용되는 열거형 값들을 문자열 배열로 반환하는 메서드
    private String[] getEnumValues(Class<?> enumClass) {
        Object[] enumValues = enumClass.getEnumConstants();
        String[] values = new String[enumValues.length];

        for (int i = 0; i < enumValues.length; i++) {
            values[i] = enumValues[i].toString();
        }

        return values;
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        ErrorResponse response = ErrorResponse.builder()
                .statusCode(HttpStatus.METHOD_NOT_ALLOWED.value())
                .details("서버에서 지원하지 않는 HTTP 메서드입니다.")
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(response);
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
