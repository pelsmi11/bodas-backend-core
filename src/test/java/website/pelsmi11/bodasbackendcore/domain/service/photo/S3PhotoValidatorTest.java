package website.pelsmi11.bodasbackendcore.domain.service.photo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3PhotoValidatorTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3PhotoValidator s3PhotoValidator;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3PhotoValidator, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3PhotoValidator, "maxUploadSizeBytes", 26214400L);
    }

    @Test
    void validateContentType_jpeg_doesNotThrow() {
        assertThatCode(() -> s3PhotoValidator.validateContentType("image/jpeg"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateContentType_png_doesNotThrow() {
        assertThatCode(() -> s3PhotoValidator.validateContentType("image/png"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateContentType_webp_doesNotThrow() {
        assertThatCode(() -> s3PhotoValidator.validateContentType("image/webp"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateContentType_invalid_throws400() {
        assertThatThrownBy(() -> s3PhotoValidator.validateContentType("video/mp4"))
                .isInstanceOf(CustomErrorException.class)
                .hasMessage("Unsupported image format");
    }

    @Test
    void validateUploadedObject_validObject_doesNotThrow() {
        when(s3Client.headObject(any(java.util.function.Consumer.class)))
                .thenReturn(HeadObjectResponse.builder()
                        .contentLength(1024L)
                        .contentType("image/jpeg")
                        .build());

        assertThatCode(() -> s3PhotoValidator.validateUploadedObject("uploads/key.jpg"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateUploadedObject_emptyObject_throws400() {
        when(s3Client.headObject(any(java.util.function.Consumer.class)))
                .thenReturn(HeadObjectResponse.builder()
                        .contentLength(0L)
                        .contentType("image/jpeg")
                        .build());

        assertThatThrownBy(() -> s3PhotoValidator.validateUploadedObject("uploads/key.jpg"))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void validateUploadedObject_oversized_throws400() {
        when(s3Client.headObject(any(java.util.function.Consumer.class)))
                .thenReturn(HeadObjectResponse.builder()
                        .contentLength(999999999L)
                        .contentType("image/jpeg")
                        .build());

        assertThatThrownBy(() -> s3PhotoValidator.validateUploadedObject("uploads/key.jpg"))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("maximum allowed size");
    }

    @Test
    void validateUploadedObject_wrongMimeType_throws400() {
        when(s3Client.headObject(any(java.util.function.Consumer.class)))
                .thenReturn(HeadObjectResponse.builder()
                        .contentLength(1024L)
                        .contentType("video/mp4")
                        .build());

        assertThatThrownBy(() -> s3PhotoValidator.validateUploadedObject("uploads/key.jpg"))
                .isInstanceOf(CustomErrorException.class)
                .hasMessage("Unsupported image format");
    }

    @Test
    void validateUploadedObject_notFound_throws404() {
        S3Exception notFound = mock(S3Exception.class);
        when(notFound.statusCode()).thenReturn(404);
        when(s3Client.headObject(any(java.util.function.Consumer.class))).thenThrow(notFound);

        assertThatThrownBy(() -> s3PhotoValidator.validateUploadedObject("uploads/missing.jpg"))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("does not exist in S3");
    }

    @Test
    void validateUploadedObject_s3Error500_rethrows() {
        S3Exception serverError = mock(S3Exception.class);
        when(serverError.statusCode()).thenReturn(500);
        when(s3Client.headObject(any(java.util.function.Consumer.class))).thenThrow(serverError);

        assertThatThrownBy(() -> s3PhotoValidator.validateUploadedObject("uploads/key.jpg"))
                .isInstanceOf(S3Exception.class);
    }
}
