package website.pelsmi11.bodasbackendcore.domain.service;

import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import website.pelsmi11.bodasbackendcore.TestDataFactory;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoConfirmRequest;
import website.pelsmi11.bodasbackendcore.domain.dto.UploadUrlResponse;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.domain.service.photo.EventResolver;
import website.pelsmi11.bodasbackendcore.domain.service.photo.GuestIdentityService;
import website.pelsmi11.bodasbackendcore.domain.service.photo.PhotoKeyFactory;
import website.pelsmi11.bodasbackendcore.domain.service.photo.S3PhotoValidator;
import website.pelsmi11.bodasbackendcore.persistence.model.Event;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;
import website.pelsmi11.bodasbackendcore.persistence.model.UserDevice;
import website.pelsmi11.bodasbackendcore.persistence.repository.PhotoRepository;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    @Mock
    private S3Template s3Template;
    @Mock
    private PhotoRepository photoRepository;
    @Mock
    private EventResolver eventResolver;
    @Mock
    private GuestIdentityService guestIdentityService;
    @Mock
    private PhotoKeyFactory photoKeyFactory;
    @Mock
    private S3PhotoValidator s3PhotoValidator;

    @InjectMocks
    private PhotoService photoService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(photoService, "bucketName", "test-bucket");
    }

    @Test
    void generateUploadUrl_happyPath_returnsUrlAndKey() throws Exception {
        Event event = TestDataFactory.event();
        UserDevice device = TestDataFactory.userDevice();
        URL signedUrl = new URL("https://s3.example.com/signed-url");
        when(eventResolver.findActiveEvent("ev123456")).thenReturn(event);
        when(guestIdentityService.resolveUserDevice("guest-1", null)).thenReturn(device);
        when(photoKeyFactory.buildUploadKey("ev123456", device.getGuestUuid(), "photo.jpg"))
                .thenReturn("uploads/ev123456/guest/photo_abc.jpg");
        when(s3Template.createSignedPutURL(eq("test-bucket"), eq("uploads/ev123456/guest/photo_abc.jpg"), eq(Duration.ofMinutes(10)), eq(null), eq("image/jpeg")))
                .thenReturn(signedUrl);

        UploadUrlResponse response = photoService.generateUploadUrl("ev123456", "guest-1", "photo.jpg", "image/jpeg", null);

        assertThat(response.getUploadUrl()).isEqualTo("https://s3.example.com/signed-url");
        assertThat(response.getS3Key()).isEqualTo("uploads/ev123456/guest/photo_abc.jpg");
    }

    @Test
    void generateUploadUrl_invalidContentType_propagatesError() {
        Event event = TestDataFactory.event();
        UserDevice device = TestDataFactory.userDevice();
        when(eventResolver.findActiveEvent("ev123456")).thenReturn(event);
        when(guestIdentityService.resolveUserDevice("guest-1", null)).thenReturn(device);
        doThrow(new CustomErrorException("Unsupported image format"))
                .when(s3PhotoValidator).validateContentType("video/mp4");

        assertThatThrownBy(() -> photoService.generateUploadUrl("ev123456", "guest-1", "photo.jpg", "video/mp4", null))
                .isInstanceOf(CustomErrorException.class)
                .hasMessage("Unsupported image format");

        verify(s3Template, never()).createSignedPutURL(any(), any(), any(), any(), any());
    }

    @Test
    void registerPendingPhoto_happyPath_persistsPhoto() {
        Event event = TestDataFactory.event();
        UserDevice device = TestDataFactory.userDevice();
        UUID guestUuid = device.getGuestUuid();
        PhotoConfirmRequest request = new PhotoConfirmRequest();
        request.setEventToken("ev123456");
        request.setGuestId(guestUuid);
        request.setS3Key("uploads/ev123456/" + guestUuid + "/photo_abc.jpg");

        when(eventResolver.findActiveEvent("ev123456")).thenReturn(event);
        when(guestIdentityService.resolveUserDevice(guestUuid, null)).thenReturn(device);
        when(photoKeyFactory.belongsToGuestAndEvent(request.getS3Key(), "ev123456", guestUuid)).thenReturn(true);

        photoService.registerPendingPhoto(request, null);

        verify(photoRepository).save(any(Photo.class));
    }

    @Test
    void registerPendingPhoto_keyNotBelongingToGuest_throws400() {
        Event event = TestDataFactory.event();
        UserDevice device = TestDataFactory.userDevice();
        UUID guestUuid = device.getGuestUuid();
        PhotoConfirmRequest request = new PhotoConfirmRequest();
        request.setEventToken("ev123456");
        request.setGuestId(guestUuid);
        request.setS3Key("uploads/other/guest/photo.jpg");

        when(eventResolver.findActiveEvent("ev123456")).thenReturn(event);
        when(guestIdentityService.resolveUserDevice(guestUuid, null)).thenReturn(device);
        when(photoKeyFactory.belongsToGuestAndEvent(request.getS3Key(), "ev123456", guestUuid)).thenReturn(false);

        assertThatThrownBy(() -> photoService.registerPendingPhoto(request, null))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("does not belong");

        verify(photoRepository, never()).save(any());
    }

    @Test
    void registerPendingPhoto_s3ValidationFails_propagatesError() {
        Event event = TestDataFactory.event();
        UserDevice device = TestDataFactory.userDevice();
        UUID guestUuid = device.getGuestUuid();
        PhotoConfirmRequest request = new PhotoConfirmRequest();
        request.setEventToken("ev123456");
        request.setGuestId(guestUuid);
        request.setS3Key("uploads/ev123456/" + guestUuid + "/photo_abc.jpg");

        when(eventResolver.findActiveEvent("ev123456")).thenReturn(event);
        when(guestIdentityService.resolveUserDevice(guestUuid, null)).thenReturn(device);
        when(photoKeyFactory.belongsToGuestAndEvent(request.getS3Key(), "ev123456", guestUuid)).thenReturn(true);
        doThrow(new CustomErrorException("Photo does not exist in S3"))
                .when(s3PhotoValidator).validateUploadedObject(request.getS3Key());

        assertThatThrownBy(() -> photoService.registerPendingPhoto(request, null))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("does not exist in S3");

        verify(photoRepository, never()).save(any());
    }
}
