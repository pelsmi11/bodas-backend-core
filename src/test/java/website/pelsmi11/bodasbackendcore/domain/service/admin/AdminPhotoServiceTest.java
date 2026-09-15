package website.pelsmi11.bodasbackendcore.domain.service.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import website.pelsmi11.bodasbackendcore.TestDataFactory;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.AdminPhotoDto;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.PhotoModerationRequest;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.PhotoStatusUpdateRequest;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.domain.service.feed.PhotoFeedPublisher;
import website.pelsmi11.bodasbackendcore.persistence.model.Event;
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;
import website.pelsmi11.bodasbackendcore.persistence.repository.EventRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.PhotoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Ownership semantics (spec 001 T025): every photo operation validates that the
 * photo belongs to an event administered by the caller; foreign access is 404.
 */
@ExtendWith(MockitoExtension.class)
class AdminPhotoServiceTest {

    private static final String SUB = TestDataFactory.TEST_ADMIN_SUB;
    private static final UUID PHOTO_ID = TestDataFactory.TEST_PHOTO_ID;
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private PhotoRepository photoRepository;
    @Mock
    private PhotoFeedPublisher photoFeedPublisher;
    @Mock
    private website.pelsmi11.bodasbackendcore.persistence.repository.EventRepository eventRepository;

    @InjectMocks
    private AdminPhotoService adminPhotoService;

    private Photo ownedPhoto(ModerationStatus status) {
        Event event = TestDataFactory.event(1, TestDataFactory.TEST_EVENT_TOKEN, true);
        event.setAdminId(SUB);
        return TestDataFactory.photo(PHOTO_ID, event, status);
    }

    private Photo foreignPhoto(ModerationStatus status) {
        Event foreign = TestDataFactory.event(2, "tok-foreign", true);
        foreign.setAdminId("other-admin");
        return TestDataFactory.photo(PHOTO_ID, foreign, status);
    }

    @Test
    void findPending_scopesToAdminEvents() {
        when(photoRepository.findByEvent_AdminIdAndStatusAndDeletedAtIsNullOrderByUploadedAtDesc(
                SUB, ModerationStatus.PENDING, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(ownedPhoto(ModerationStatus.PENDING))));

        Page<AdminPhotoDto> result = adminPhotoService.findPending(SUB, PAGEABLE);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findById_owned_returns() {
        when(photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(ownedPhoto(ModerationStatus.PENDING)));
        AdminPhotoDto dto = adminPhotoService.findById(SUB, PHOTO_ID);
        assertThat(dto.id()).isEqualTo(PHOTO_ID);
    }

    @Test
    void findById_foreign_returns404() {
        when(photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(foreignPhoto(ModerationStatus.PENDING)));
        assertThatThrownBy(() -> adminPhotoService.findById(SUB, PHOTO_ID))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("Photo not found");
    }

    @Test
    void updateStatus_owned_approvesAndPublishes() {
        Photo photo = ownedPhoto(ModerationStatus.PENDING);
        when(photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(photo));
        when(photoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PhotoStatusUpdateRequest request = new PhotoStatusUpdateRequest();
        request.setStatus(ModerationStatus.APPROVED);
        AdminPhotoDto dto = adminPhotoService.updateStatus(SUB, PHOTO_ID, request);

        assertThat(dto.status()).isEqualTo(ModerationStatus.APPROVED);
        verify(photoFeedPublisher).publishApprovedPhoto(photo);
    }

    @Test
    void updateStatus_foreign_returns404() {
        when(photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(foreignPhoto(ModerationStatus.PENDING)));
        PhotoStatusUpdateRequest request = new PhotoStatusUpdateRequest();
        request.setStatus(ModerationStatus.APPROVED);

        assertThatThrownBy(() -> adminPhotoService.updateStatus(SUB, PHOTO_ID, request))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("Photo not found");
    }

    @Test
    void moderate_ownedPhotos_updatesAll() {
        Photo owned = ownedPhoto(ModerationStatus.PENDING);
        when(photoRepository.findByIdIn(any())).thenReturn(List.of(owned));
        when(photoRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        PhotoModerationRequest request = new PhotoModerationRequest();
        request.setPhotoIds(List.of(PHOTO_ID));
        request.setAction(PhotoModerationRequest.ModerationAction.APPROVE);
        List<AdminPhotoDto> result = adminPhotoService.moderate(SUB, request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(ModerationStatus.APPROVED);
        verify(photoFeedPublisher).publishApprovedPhoto(owned);
    }

    @Test
    void moderate_foreignPhoto_returns404() {
        when(photoRepository.findByIdIn(any())).thenReturn(List.of(foreignPhoto(ModerationStatus.PENDING)));
        PhotoModerationRequest request = new PhotoModerationRequest();
        request.setPhotoIds(List.of(PHOTO_ID));

        assertThatThrownBy(() -> adminPhotoService.moderate(SUB, request))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("Photo not found");
    }

    @Test
    void softDelete_owned_deletes() {
        when(photoRepository.findByIdIn(any())).thenReturn(List.of(ownedPhoto(ModerationStatus.PENDING)));
        when(photoRepository.softDeleteByIds(any(), any(java.time.OffsetDateTime.class))).thenReturn(1);

        int count = adminPhotoService.softDelete(SUB, PHOTO_ID.toString());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void softDelete_foreign_returns404() {
        when(photoRepository.findByIdIn(any())).thenReturn(List.of(foreignPhoto(ModerationStatus.PENDING)));
        assertThatThrownBy(() -> adminPhotoService.softDelete(SUB, PHOTO_ID.toString()))
                .isInstanceOf(CustomErrorException.class);
    }
}
