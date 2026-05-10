package website.pelsmi11.bodasbackendcore.domain.exception;

import org.springframework.http.HttpStatus;

public class CustomErrorException extends RuntimeException {

    private final HttpStatus status;

    public CustomErrorException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public CustomErrorException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public CustomErrorException(String message, int statusCode) {
        this(message, HttpStatus.valueOf(statusCode));
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static CustomErrorException handlerCustomError(String message) {
        return new CustomErrorException(message);
    }

    public static CustomErrorException handlerCustomError(String message, HttpStatus status) {
        return new CustomErrorException(message, status);
    }

    public static CustomErrorException handlerCustomError(String message, int statusCode) {
        return new CustomErrorException(message, statusCode);
    }
}
