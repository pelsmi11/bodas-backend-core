package website.pelsmi11.bodasbackendcore.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import website.pelsmi11.bodasbackendcore.TestDataFactory;
import website.pelsmi11.bodasbackendcore.domain.dto.ModerationWebhookRequest;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.domain.service.feed.PhotoFeedPublisher;
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;
import website.pelsmi11.bodasbackendcore.persistence.repository.PhotoRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock
    private PhotoRepository photoRepository;
    @Mock
    private PhotoFeedPublisher photoFeedPublisher;

    @InjectMocks
    private ModerationService moderationService;

    @Test
    void processModerationResult_approvedWithReason_setsStatusAndPublishes() {
        Photo photo = TestDataFactory.photo();
        when(photoRepository.findFirstByS3Key(photo.getS3Key())).thenReturn(Optional.of(photo));
        when(photoRepository.save(any(Photo.class))).thenAnswer(inv -> inv.getArgument(0));

        moderationService.processModerationResult(
                new ModerationWebhookRequest(photo.getS3Key(), true, "inappropriate content"));

        assertThat(photo.getStatus()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(photo.getModerationDetails()).containsEntry("reason", "inappropriate content");
        assertThat(photo.getModerationDetails()).containsEntry("source", "lambda-webhook");
        verify(photoFeedPublisher).publishApprovedPhoto(photo);
    }

    @Test
    void processModerationResult_rejected_doesNotPublish() {
        Photo photo = TestDataFactory.photo();
        when(photoRepository.findFirstByS3Key(photo.getS3Key())).thenReturn(Optional.of(photo));
        when(photoRepository.save(any(Photo.class))).thenAnswer(inv -> inv.getArgument(0));

        moderationService.processModerationResult(
                new ModerationWebhookRequest(photo.getS3Key(), false, "rejected reason"));

        assertThat(photo.getStatus()).isEqualTo(ModerationStatus.REJECTED);
        verify(photoFeedPublisher, never()).publishApprovedPhoto(any());
    }

    @Test
    void processModerationResult_blankReason_doesNotSetDetails() {
        Photo photo = TestDataFactory.photo();
        when(photoRepository.findFirstByS3Key(photo.getS3Key())).thenReturn(Optional.of(photo));
        when(photoRepository.save(any(Photo.class))).thenAnswer(inv -> inv.getArgument(0));

        moderationService.processModerationResult(
                new ModerationWebhookRequest(photo.getS3Key(), true, "  "));

        assertThat(photo.getStatus()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(photo.getModerationDetails()).isNull();
    }

    @Test
    void processModerationResult_nullReason_doesNotSetDetails() {
        Photo photo = TestDataFactory.photo();
        when(photoRepository.findFirstByS3Key(photo.getS3Key())).thenReturn(Optional.of(photo));
        when(photoRepository.save(any(Photo.class))).thenAnswer(inv -> inv.getArgument(0));

        moderationService.processModerationResult(
                new ModerationWebhookRequest(photo.getS3Key(), true, null));

        assertThat(photo.getStatus()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(photo.getModerationDetails()).isNull();
    }

    @Test
    void processModerationResult_photoNotFound_throws404() {
        when(photoRepository.findFirstByS3Key("missing-key")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moderationService.processModerationResult(
                new ModerationWebhookRequest("missing-key", true, null)))
                .isInstanceOf(CustomErrorException.class)
                .hasMessage("Photo not found");

        verify(photoFeedPublisher, never()).publishApprovedPhoto(any());
    }
}
