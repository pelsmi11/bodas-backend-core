package website.pelsmi11.bodasbackendcore.domain.service;

import io.awspring.cloud.s3.S3Template;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoConfirmRequest;
import website.pelsmi11.bodasbackendcore.domain.dto.UploadUrlResponse;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.domain.service.photo.EventResolver;
import website.pelsmi11.bodasbackendcore.domain.service.photo.GuestIdentityService;
import website.pelsmi11.bodasbackendcore.domain.service.photo.PhotoKeyFactory;
import website.pelsmi11.bodasbackendcore.domain.service.photo.S3PhotoValidator;
import website.pelsmi11.bodasbackendcore.persistence.model.Event;
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;
import website.pelsmi11.bodasbackendcore.persistence.model.UserDevice;
import website.pelsmi11.bodasbackendcore.persistence.repository.PhotoRepository;

import java.time.Duration;

/**
 * Application service orchestrating the guest upload + confirmation flow.
 * Business details are delegated to focused collaborators.
 */
@Service
public class PhotoService {

    private final S3Template s3Template;
    private final PhotoRepository photoRepository;
    private final EventResolver eventResolver;
    private final GuestIdentityService guestIdentityService;
    private final PhotoKeyFactory photoKeyFactory;
    private final S3PhotoValidator s3PhotoValidator;

    @Value("${app.s3.bucket-name}")
    private String bucketName;

    public PhotoService(
            S3Template s3Template,
            PhotoRepository photoRepository,
            EventResolver eventResolver,
            GuestIdentityService guestIdentityService,
            PhotoKeyFactory photoKeyFactory,
            S3PhotoValidator s3PhotoValidator
    ) {
        this.s3Template = s3Template;
        this.photoRepository = photoRepository;
        this.eventResolver = eventResolver;
        this.guestIdentityService = guestIdentityService;
        this.photoKeyFactory = photoKeyFactory;
        this.s3PhotoValidator = s3PhotoValidator;
    }

    /**
     * Generates a signed PUT URL and the canonical S3 key for the upload.
     */
    @Transactional
    public UploadUrlResponse generateUploadUrl(
            String eventToken,
            String guestIdRaw,
            String fileName,
            String contentType,
            String cognitoId
    ) {
        Event event = eventResolver.findActiveEvent(eventToken);
        UserDevice userDevice = guestIdentityService.resolveUserDevice(guestIdRaw, cognitoId);
        s3PhotoValidator.validateContentType(contentType);

        String s3Key = photoKeyFactory.buildUploadKey(event.getToken(), userDevice.getGuestUuid(), fileName);
        String uploadUrl = s3Template.createSignedPutURL(
                bucketName,
                s3Key,
                Duration.ofMinutes(10),
                null,
                contentType
        ).toString();

        return new UploadUrlResponse(uploadUrl, s3Key);
    }

    /**
     * Confirms a completed upload and persists a pending moderation record.
     */
    @Transactional
    public void registerPendingPhoto(PhotoConfirmRequest request, String cognitoId) {
        Event event = eventResolver.findActiveEvent(request.getEventToken());
        UserDevice userDevice = guestIdentityService.resolveUserDevice(request.getGuestId(), cognitoId);

        if (!photoKeyFactory.belongsToGuestAndEvent(request.getS3Key(), event.getToken(), userDevice.getGuestUuid())) {
            throw CustomErrorException.handlerCustomError("La foto no pertenece al evento o dispositivo indicado", HttpStatus.BAD_REQUEST);
        }

        s3PhotoValidator.validateUploadedObject(request.getS3Key());

        Photo photo = new Photo();
        photo.setEvent(event);
        photo.setUser(userDevice.getUser());
        photo.setDevice(userDevice);
        photo.setS3Key(request.getS3Key());
        photo.setStatus(ModerationStatus.PENDING);

        photoRepository.save(photo);
    }
}
