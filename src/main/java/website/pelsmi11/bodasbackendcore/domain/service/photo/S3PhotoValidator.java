package website.pelsmi11.bodasbackendcore.domain.service.photo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;

import java.util.Set;

/**
 * Validates upload MIME and confirms uploaded object presence/size in S3.
 */
@Component
public class S3PhotoValidator {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final S3Client s3Client;

    @Value("${app.s3.bucket-name}")
    private String bucketName;

    @Value("${app.photos.max-upload-size-bytes:26214400}")
    private long maxUploadSizeBytes;

    public S3PhotoValidator(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Validates request MIME type against supported image formats.
     */
    public void validateContentType(String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw CustomErrorException.handlerCustomError("Formato de imagen no soportado", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Verifies that the uploaded S3 object exists, is non-empty, within size limits,
     * and still has an allowed image MIME type.
     */
    public void validateUploadedObject(String s3Key) {
        try {
            HeadObjectResponse object = s3Client.headObject(request -> request
                    .bucket(bucketName)
                    .key(s3Key)
            );

            if (object.contentLength() <= 0) {
                throw CustomErrorException.handlerCustomError("La foto subida esta vacia", HttpStatus.BAD_REQUEST);
            }

            if (object.contentLength() > maxUploadSizeBytes) {
                throw CustomErrorException.handlerCustomError("La foto excede el tamano maximo permitido", HttpStatus.BAD_REQUEST);
            }

            validateContentType(object.contentType());
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw CustomErrorException.handlerCustomError("La foto no existe en S3", HttpStatus.NOT_FOUND);
            }
            throw exception;
        }
    }
}
