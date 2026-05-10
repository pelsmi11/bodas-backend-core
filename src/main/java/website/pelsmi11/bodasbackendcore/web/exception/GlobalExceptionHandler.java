package website.pelsmi11.bodasbackendcore.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import website.pelsmi11.bodasbackendcore.domain.dto.ApiError;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomErrorException.class)
    public ResponseEntity<ApiError> handleCustomError(CustomErrorException exception) {
        return buildError(exception.getMessage(), exception.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationError(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> String.format("%s %s", error.getField(), error.getDefaultMessage()))
                .orElse("Invalid request");

        return buildError(message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedError(Exception exception) {
        return buildError("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiError> buildError(String message, HttpStatus status) {
        return ResponseEntity.status(status).body(new ApiError(false, message));
    }
}
