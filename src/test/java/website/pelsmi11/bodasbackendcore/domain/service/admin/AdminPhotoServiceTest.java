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
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;
import website.pelsmi11.bodasbackendcore.persistence.repository.PhotoRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPhotoServiceTest {

    private static final UUID PHOTO_ID = TestDataFactory.TEST_PHOTO_ID;

    @Mock
    private PhotoRepository photoRepository;
    @Mock
    private PhotoFeedPublisher photoFeedPublisher;

    @InjectMocks
    private AdminPhotoService adminPhotoService;

    @Test
    void findByEvent_withStatus_callsFilteredQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Photo> page = new PageImpl<>(List.of(TestDataFactory.photo()));
        when(photoRepository.findByEventIdAndStatusAndDeletedAtIsNullOrderByUploadedAtDesc(1, ModerationStatus.PENDING, pageable))
                .thenReturn(page);

        Page<AdminPhotoDto> result = adminPhotoService.findByEvent(1, ModerationStatus.PENDING, false, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findByEvent_withoutStatus_notDeleted_callsNonDeletedQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Photo> page = new PageImpl<>(List.of());
        when(photoRepository.findByEventIdAndDeletedAtIsNullOrderByUploadedAtDesc(1, pageable)).thenReturn(page);

        adminPhotoService.findByEvent(1, null, false, pageable);

        verify(photoRepository).findByEventIdAndDeletedAtIsNullOrderByUploadedAtDesc(1, pageable);
    }

    @Test
    void findByEvent_includeDeleted_callsAllQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Photo> page = new PageImpl<>(List.of());
        when(photoRepository.findByEventIdOrderByUploadedAtDesc(1, pageable)).thenReturn(page);

        adminPhotoService.findByEvent(1, null, true, pageable);

        verify(photoRepository).findByEventIdOrderByUploadedAtDesc(1, pageable);
    }

    @Test
    void findPendingGlobal_returnsPendingPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Photo> page = new PageImpl<>(List.of(TestDataFactory.photo(PHOTO_ID, TestDataFactory.event(), ModerationStatus.PENDING)));
        when(photoRepository.findByStatusAndDeletedAtIsNullOrderByUploadedAtDesc(ModerationStatus.PENDING, pageable))
                .thenReturn(page);

        Page<AdminPhotoDto> result = adminPhotoService.findPendingGlobal(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findById_existing_returnsDto() {
        Photo photo = TestDataFactory.photo();
        when(photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(photo));

        AdminPhotoDto dto = adminPhotoService.findById(PHOTO_ID);

        assertThat(dto.id()).isEqualTo(PHOTO_ID);
        assertThat(dto.s3Key()).isEqualTo(photo.getS3Key());
    }

    @Test
    void findById_notFound_throws404() {
        when(photoRepository.findById(PHOTO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPhotoService.findById(PHOTO_ID))
                .isInstanceOf(CustomErrorException.class)
                .hasMessage("Photo not found");
    }

    @Test
    void updateStatus_approved_publishesFeed() {
        Photo photo = TestDataFactory.photo(PHOTO_ID, TestDataFactory.event(), ModerationStatus.PENDING);
        PhotoStatusUpdateRequest request = new PhotoStatusUpdateRequest();
        request.setStatus(ModerationStatus.APPROVED);
        request.setReason("manual approval");
        when(photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(photo));
        when(photoRepository.save(any(Photo.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminPhotoDto dto = adminPhotoService.updateStatus(PHOTO_ID, request, "admin-sub");

        assertThat(dto.status()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(dto.moderatedBy()).isEqualTo("admin-sub");
        assertThat(dto.moderationDetails()).containsEntry("source", "admin-manual");
        assertThat(dto.moderationDetails()).containsEntry("reason", "manual approval");
        assertThat(dto.moderationDetails()).containsEntry("previousStatus", "PENDING");
        verify(photoFeedPublisher).publishApprovedPhoto(photo);
    }

    @Test
    void updateStatus_rejected_doesNotPublish() {
        Photo photo = TestDataFactory.photo(PHOTO_ID, TestDataFactory.event(), ModerationStatus.PENDING);
        PhotoStatusUpdateRequest request = new PhotoStatusUpdateRequest();
        request.setStatus(ModerationStatus.REJECTED);
        when(photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(photo));
        when(photoRepository.save(any(Photo.class))).thenAnswer(inv -> inv.getArgument(0));

        adminPhotoService.updateStatus(PHOTO_ID, request, "admin-sub");

        verify(photoFeedPublisher, never()).publishApprovedPhoto(any());
    }

    @Test
    void moderate_approveBatch_publishesEach() {
        Photo photo1 = TestDataFactory.photo(UUID.fromString("22222222-2222-2222-2222-222222222222"), TestDataFactory.event(), ModerationStatus.PENDING);
        Photo photo2 = TestDataFactory.photo(UUID.fromString("33333333-3333-3333-3333-333333333333"), TestDataFactory.event(), ModerationStatus.PENDING);
        PhotoModerationRequest request = new PhotoModerationRequest();
        request.setPhotoIds(List.of(photo1.getId(), photo2.getId()));
        request.setAction(PhotoModerationRequest.ModerationAction.APPROVE);
        when(photoRepository.findByIdIn(request.getPhotoIds())).thenReturn(List.of(photo1, photo2));
        when(photoRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<AdminPhotoDto> result = adminPhotoService.moderate(request, "admin-sub");

        assertThat(result).hasSize(2);
        assertThat(photo1.getStatus()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(photo1.getModeratedBy()).isEqualTo("admin-sub");
        verify(photoFeedPublisher, times(2)).publishApprovedPhoto(any());
    }

    @Test
    void moderate_rejectBatch_doesNotPublish() {
        Photo photo = TestDataFactory.photo(PHOTO_ID, TestDataFactory.event(), ModerationStatus.PENDING);
        PhotoModerationRequest request = new PhotoModerationRequest();
        request.setPhotoIds(List.of(PHOTO_ID));
        request.setAction(PhotoModerationRequest.ModerationAction.REJECT);
        request.setReason("inappropriate");
        when(photoRepository.findByIdIn(request.getPhotoIds())).thenReturn(List.of(photo));
        when(photoRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        adminPhotoService.moderate(request, "admin-sub");

        assertThat(photo.getStatus()).isEqualTo(ModerationStatus.REJECTED);
        verify(photoFeedPublisher, never()).publishApprovedPhoto(any());
    }

    @Test
    void moderate_noPhotosFound_throws404() {
        PhotoModerationRequest request = new PhotoModerationRequest();
        request.setPhotoIds(List.of(PHOTO_ID));
        request.setAction(PhotoModerationRequest.ModerationAction.APPROVE);
        when(photoRepository.findByIdIn(request.getPhotoIds())).thenReturn(List.of());

        assertThatThrownBy(() -> adminPhotoService.moderate(request, "admin-sub"))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void softDelete_withValidIds_callsRepository() {
        List<UUID> ids = List.of(PHOTO_ID);
        when(photoRepository.softDeleteByIds(eq(ids), any(OffsetDateTime.class))).thenReturn(1);

        int count = adminPhotoService.softDelete(PHOTO_ID.toString());

        assertThat(count).isEqualTo(1);
    }

    @Test
    void softDelete_emptyString_throws400() {
        assertThatThrownBy(() -> adminPhotoService.softDelete(""))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("required");
        verify(photoRepository, never()).softDeleteByIds(any(), any());
    }

    @Test
    void softDelete_nullString_throws400() {
        assertThatThrownBy(() -> adminPhotoService.softDelete(null))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("required");
        verify(photoRepository, never()).softDeleteByIds(any(), any());
    }

    @Test
    void softDelete_invalidUuid_throws400() {
        assertThatThrownBy(() -> adminPhotoService.softDelete("not-a-uuid"))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("Invalid UUID");
        verify(photoRepository, never()).softDeleteByIds(any(), any());
    }

    @Test
    void softDelete_blankString_throws400() {
        assertThatThrownBy(() -> adminPhotoService.softDelete("   "))
                .isInstanceOf(CustomErrorException.class);
        verify(photoRepository, never()).softDeleteByIds(any(), any());
    }
}
