package website.pelsmi11.bodasbackendcore.domain.service.feed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import website.pelsmi11.bodasbackendcore.TestDataFactory;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoFeedDto;
import website.pelsmi11.bodasbackendcore.domain.dto.PublicEventDto;
import website.pelsmi11.bodasbackendcore.domain.service.photo.EventResolver;
import website.pelsmi11.bodasbackendcore.persistence.model.Event;
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;
import website.pelsmi11.bodasbackendcore.persistence.repository.PhotoRepository;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventFeedServiceTest {

    @Mock private PhotoRepository photoRepository;
    @Mock private EventResolver eventResolver;
    @Mock private PhotoFeedMapper photoFeedMapper;

    private EventFeedService eventFeedService;

    @BeforeEach
    void setUp() {
        eventFeedService = new EventFeedService(photoRepository, eventResolver, photoFeedMapper, 120);
    }

    @Test
    void getEventDetails_returnsOnlyPublicEventData() {
        Event event = TestDataFactory.event();
        event.setEventDate(OffsetDateTime.parse("2026-01-01T18:00:00Z"));
        event.setDescription("Jardin");
        when(eventResolver.findActiveEvent("ev123456")).thenReturn(event);

        PublicEventDto result = eventFeedService.getEventDetails("ev123456");

        assertThat(result.eventToken()).isEqualTo("ev123456");
        assertThat(result.name()).isEqualTo("Boda Test");
        assertThat(result.description()).isEqualTo("Jardin");
    }

    @Test
    void getApprovedFeed_validatesEventLimitsResultsAndMapsDtos() {
        Photo photo = TestDataFactory.photo();
        PhotoFeedDto dto = new PhotoFeedDto("photo-id", "https://s3.example.com/signed", "2026-01-01T00:00:00Z");
        when(eventResolver.findActiveEvent("ev123456")).thenReturn(TestDataFactory.event());
        when(photoRepository.findByEventTokenAndStatusAndDeletedAtIsNullOrderByUploadedAtDesc(
                eq("ev123456"), eq(ModerationStatus.APPROVED), any(Pageable.class)
        )).thenReturn(List.of(photo));
        when(photoFeedMapper.toPublicDto(photo)).thenReturn(dto);

        assertThat(eventFeedService.getApprovedFeed("ev123456")).containsExactly(dto);
        verify(photoRepository).findByEventTokenAndStatusAndDeletedAtIsNullOrderByUploadedAtDesc(
                eq("ev123456"),
                eq(ModerationStatus.APPROVED),
                argThat(pageable -> pageable.getPageSize() == 120 && pageable.getPageNumber() == 0)
        );
    }

    @Test
    void getApprovedFeed_emptyList_returnsEmpty() {
        when(eventResolver.findActiveEvent("ev123456")).thenReturn(TestDataFactory.event());
        when(photoRepository.findByEventTokenAndStatusAndDeletedAtIsNullOrderByUploadedAtDesc(
                eq("ev123456"), eq(ModerationStatus.APPROVED), any(Pageable.class)
        )).thenReturn(List.of());

        assertThat(eventFeedService.getApprovedFeed("ev123456")).isEmpty();
    }
}
