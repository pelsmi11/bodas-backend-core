package website.pelsmi11.bodasbackendcore.domain.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class CustomErrorExceptionTest {

    @Test
    void constructor_defaultStatus_isBadRequest() {
        CustomErrorException exception = new CustomErrorException("bad input");

        assertThat(exception.getMessage()).isEqualTo("bad input");
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void constructor_withHttpStatus_setsStatus() {
        CustomErrorException exception = new CustomErrorException("not found", HttpStatus.NOT_FOUND);

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void constructor_withStatusCode_setsStatus() {
        CustomErrorException exception = new CustomErrorException("server error", 500);

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void factoryHandlerCustomError_withMessage_returnsBadRequest() {
        CustomErrorException exception = CustomErrorException.handlerCustomError("default bad");

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getMessage()).isEqualTo("default bad");
    }

    @Test
    void factoryHandlerCustomError_withHttpStatus_setsStatus() {
        CustomErrorException exception = CustomErrorException.handlerCustomError("conflict", HttpStatus.CONFLICT);

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void factoryHandlerCustomError_withStatusCode_setsStatus() {
        CustomErrorException exception = CustomErrorException.handlerCustomError("forbidden", 403);

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
