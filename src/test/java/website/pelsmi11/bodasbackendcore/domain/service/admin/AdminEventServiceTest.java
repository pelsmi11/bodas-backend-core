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
import website.pelsmi11.bodasbackendcore.domain.dto.admin.EventPhotoStatsDto;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.EventCreateRequest;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.EventResponse;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.EventTokenRegenerateResponse;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.EventUpdateRequest;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.persistence.model.Event;
import website.pelsmi11.bodasbackendcore.persistence.model.EventStatus;
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;
import website.pelsmi11.bodasbackendcore.persistence.repository.EventRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.PhotoRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Ownership semantics (spec 001 T025): every operation is scoped to
 * events.admin_id == caller sub; browser-supplied adminId is ignored; foreign
 * event access is indistinguishable from missing (404).
 */
@ExtendWith(MockitoExtension.class)
class AdminEventServiceTest {

    private static final String SUB = TestDataFactory.TEST_ADMIN_SUB;

    @Mock
    private EventRepository eventRepository;
    @Mock
    private PhotoRepository photoRepository;
    @Mock
    private EventTokenGenerator tokenGenerator;

    @InjectMocks
    private AdminEventService adminEventService;

    private Event ownedEvent() {
        return TestDataFactory.event(1, TestDataFactory.TEST_EVENT_TOKEN, true);
    }

    private Event foreignEvent() {
        Event event = TestDataFactory.event(2, "tok-foreign", true);
        event.setAdminId("other-admin-sub");
        return event;
    }

    @Test
    void create_derivesAdminIdFromJwtSub_ignoresBrowserAdminId() {
        EventCreateRequest request = new EventCreateRequest();
        request.setName("Boda Juan y Maria");
        when(tokenGenerator.generateUniqueToken()).thenReturn("tok12345");
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId(1);
            return e;
        });
        when(photoRepository.countByEventId(1)).thenReturn(0L);

        EventResponse response = adminEventService.create(request, SUB);

        assertThat(response.adminId()).isEqualTo(SUB);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void create_withoutAdminId_usesJwtSub() {
        EventCreateRequest request = new EventCreateRequest();
        request.setName("Boda");
        when(tokenGenerator.generateUniqueToken()).thenReturn("tok12345");
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId(1);
            return e;
        });
        when(photoRepository.countByEventId(1)).thenReturn(0L);

        EventResponse response = adminEventService.create(request, SUB);
        assertThat(response.adminId()).isEqualTo(SUB);
    }

    @Test
    void findAll_scopesByAdminId() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Event> page = new PageImpl<>(List.of(ownedEvent()));
        when(eventRepository.findByAdminIdOrderByCreatedAtDesc(SUB, pageable)).thenReturn(page);
        when(photoRepository.countByEventId(1)).thenReturn(2L);

        Page<EventResponse> result = adminEventService.findAll(SUB, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(eventRepository).findByAdminIdOrderByCreatedAtDesc(SUB, pageable);
    }

    @Test
    void findById_ownedEvent_returnsResponse() {
        when(eventRepository.findById(1)).thenReturn(Optional.of(ownedEvent()));
        when(photoRepository.countByEventId(1)).thenReturn(1L);

        EventResponse response = adminEventService.findById(SUB, 1);
        assertThat(response.id()).isEqualTo(1);
    }

    @Test
    void findById_foreignEvent_returns404() {
        when(eventRepository.findById(2)).thenReturn(Optional.of(foreignEvent()));

        assertThatThrownBy(() -> adminEventService.findById(SUB, 2))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("Event not found");
    }

    @Test
    void update_ownedEvent_appliesChanges() {
        when(eventRepository.findById(1)).thenReturn(Optional.of(ownedEvent()));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(photoRepository.countByEventId(1)).thenReturn(1L);

        EventUpdateRequest request = new EventUpdateRequest();
        request.setName("Nuevo nombre");
        EventResponse response = adminEventService.update(SUB, 1, request);
        assertThat(response.name()).isEqualTo("Nuevo nombre");
    }

    @Test
    void update_foreignEvent_returns404() {
        when(eventRepository.findById(2)).thenReturn(Optional.of(foreignEvent()));
        assertThatThrownBy(() -> adminEventService.update(SUB, 2, new EventUpdateRequest()))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("Event not found");
    }

    @Test
    void regenerateToken_ownedEvent_rotates() {
        when(eventRepository.findById(1)).thenReturn(Optional.of(ownedEvent()));
        when(tokenGenerator.generateUniqueToken()).thenReturn("newtoken1");
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventTokenRegenerateResponse response = adminEventService.regenerateToken(SUB, 1);
        assertThat(response.token()).isEqualTo("newtoken1");
    }

    @Test
    void regenerateToken_foreignEvent_returns404() {
        when(eventRepository.findById(2)).thenReturn(Optional.of(foreignEvent()));
        assertThatThrownBy(() -> adminEventService.regenerateToken(SUB, 2))
                .isInstanceOf(CustomErrorException.class);
    }

    @Test
    void setActive_ownedEvent_changesStatus() {
        when(eventRepository.findById(1)).thenReturn(Optional.of(ownedEvent()));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(photoRepository.countByEventId(1)).thenReturn(0L);

        EventResponse response = adminEventService.setActive(SUB, 1, false);
        assertThat(response.isActive()).isFalse();
    }

    @Test
    void delete_ownedEvent_archives() {
        Event event = ownedEvent();
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        adminEventService.delete(SUB, 1);

        assertThat(event.getStatus()).isEqualTo(EventStatus.ARCHIVED);
        assertThat(event.getIsActive()).isFalse();
    }

    @Test
    void delete_foreignEvent_returns404() {
        when(eventRepository.findById(2)).thenReturn(Optional.of(foreignEvent()));
        assertThatThrownBy(() -> adminEventService.delete(SUB, 2))
                .isInstanceOf(CustomErrorException.class);
    }

    @Test
    void getStats_ownedEvent_counts() {
        when(eventRepository.findById(1)).thenReturn(Optional.of(ownedEvent()));
        when(photoRepository.countByEventId(1)).thenReturn(5L);
        when(photoRepository.countByEventIdAndStatus(1, ModerationStatus.PENDING)).thenReturn(2L);
        when(photoRepository.countByEventIdAndStatus(1, ModerationStatus.APPROVED)).thenReturn(2L);
        when(photoRepository.countByEventIdAndStatus(1, ModerationStatus.REJECTED)).thenReturn(1L);
        when(photoRepository.countByEventIdAndDeletedAtIsNotNull(1)).thenReturn(0L);

        EventPhotoStatsDto stats = adminEventService.getStats(SUB, 1);
        assertThat(stats.total()).isEqualTo(5L);
    }
}
