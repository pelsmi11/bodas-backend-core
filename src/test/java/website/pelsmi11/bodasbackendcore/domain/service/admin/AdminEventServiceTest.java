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
import website.pelsmi11.bodasbackendcore.domain.dto.admin.EventCreateRequest;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.EventPhotoStatsDto;
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

@ExtendWith(MockitoExtension.class)
class AdminEventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private PhotoRepository photoRepository;
    @Mock
    private EventTokenGenerator tokenGenerator;

    @InjectMocks
    private AdminEventService adminEventService;

    @Test
    void create_withExplicitAdminId_usesThatId() {
        EventCreateRequest request = new EventCreateRequest();
        request.setName("Boda Juan y Maria");
        request.setAdminId("custom-admin-id");
        when(tokenGenerator.generateUniqueToken()).thenReturn("tok12345");
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId(1);
            return e;
        });
        when(photoRepository.countByEventId(1)).thenReturn(0L);

        EventResponse response = adminEventService.create(request, "jwt-sub");

        assertThat(response.adminId()).isEqualTo("custom-admin-id");
        assertThat(response.token()).isEqualTo("tok12345");
        assertThat(response.status()).isEqualTo(EventStatus.ACTIVE);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void create_withoutAdminId_usesJwtSub() {
        EventCreateRequest request = new EventCreateRequest();
        request.setName("Boda Test");
        when(tokenGenerator.generateUniqueToken()).thenReturn("tok12345");
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId(1);
            return e;
        });
        when(photoRepository.countByEventId(1)).thenReturn(0L);

        EventResponse response = adminEventService.create(request, "jwt-sub-123");

        assertThat(response.adminId()).isEqualTo("jwt-sub-123");
    }

    @Test
    void findById_existing_returnsResponse() {
        Event event = TestDataFactory.event();
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(photoRepository.countByEventId(1)).thenReturn(5L);

        EventResponse response = adminEventService.findById(1);

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.photoCount()).isEqualTo(5L);
    }

    @Test
    void findById_notFound_throws404() {
        when(eventRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminEventService.findById(99))
                .isInstanceOf(CustomErrorException.class)
                .hasMessage("Event not found");
    }

    @Test
    void findAll_withSearchAndStatus_callsCombinedQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Event> page = new PageImpl<>(List.of(TestDataFactory.event()));
        when(eventRepository.findByNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc("boda", EventStatus.ACTIVE, pageable))
                .thenReturn(page);
        when(photoRepository.countByEventId(anyInt())).thenReturn(0L);

        Page<EventResponse> result = adminEventService.findAll("boda", EventStatus.ACTIVE, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findAll_withOnlyStatus_callsStatusQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Event> page = new PageImpl<>(List.of());
        when(eventRepository.findByStatusOrderByCreatedAtDesc(EventStatus.ACTIVE, pageable)).thenReturn(page);

        adminEventService.findAll(null, EventStatus.ACTIVE, pageable);

        verify(eventRepository).findByStatusOrderByCreatedAtDesc(EventStatus.ACTIVE, pageable);
    }

    @Test
    void findAll_withOnlySearch_callsSearchQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Event> page = new PageImpl<>(List.of());
        when(eventRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc("boda", pageable)).thenReturn(page);

        adminEventService.findAll("boda", null, pageable);

        verify(eventRepository).findByNameContainingIgnoreCaseOrderByCreatedAtDesc("boda", pageable);
    }

    @Test
    void findAll_withNoFilters_callsFindAll() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Event> page = new PageImpl<>(List.of());
        when(eventRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(page);

        adminEventService.findAll(null, null, pageable);

        verify(eventRepository).findAllByOrderByCreatedAtDesc(pageable);
    }

    @Test
    void update_partialMerge_updatesOnlyProvidedFields() {
        Event event = TestDataFactory.event();
        EventUpdateRequest request = new EventUpdateRequest();
        request.setName("Updated Name");
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(photoRepository.countByEventId(1)).thenReturn(0L);

        EventResponse response = adminEventService.update(1, request);

        assertThat(response.name()).isEqualTo("Updated Name");
    }

    @Test
    void update_invalidStatus_throws400() {
        Event event = TestDataFactory.event();
        EventUpdateRequest request = new EventUpdateRequest();
        request.setStatus("INVALID_STATUS");
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> adminEventService.update(1, request))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("Invalid event status");
    }

    @Test
    void update_notFound_throws404() {
        when(eventRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminEventService.update(99, new EventUpdateRequest()))
                .isInstanceOf(CustomErrorException.class);
    }

    @Test
    void regenerateToken_generatesNewToken() {
        Event event = TestDataFactory.event();
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(tokenGenerator.generateUniqueToken()).thenReturn("newtok99");
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventTokenRegenerateResponse response = adminEventService.regenerateToken(1);

        assertThat(response.token()).isEqualTo("newtok99");
    }

    @Test
    void setActive_false_setsArchivedStatus() {
        Event event = TestDataFactory.event();
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(photoRepository.countByEventId(1)).thenReturn(0L);

        EventResponse response = adminEventService.setActive(1, false);

        assertThat(response.isActive()).isFalse();
        assertThat(response.status()).isEqualTo(EventStatus.ARCHIVED);
    }

    @Test
    void setActive_true_archivedToActive_restoresActiveStatus() {
        Event event = TestDataFactory.event();
        event.setStatus(EventStatus.ARCHIVED);
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(photoRepository.countByEventId(1)).thenReturn(0L);

        EventResponse response = adminEventService.setActive(1, true);

        assertThat(response.isActive()).isTrue();
        assertThat(response.status()).isEqualTo(EventStatus.ACTIVE);
    }

    @Test
    void delete_setsInactiveAndArchived() {
        Event event = TestDataFactory.event();
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        adminEventService.delete(1);

        assertThat(event.getIsActive()).isFalse();
        assertThat(event.getStatus()).isEqualTo(EventStatus.ARCHIVED);
    }

    @Test
    void getStats_returnsCountsByStatus() {
        when(eventRepository.findById(1)).thenReturn(Optional.of(TestDataFactory.event()));
        when(photoRepository.countByEventId(1)).thenReturn(100L);
        when(photoRepository.countByEventIdAndStatus(1, ModerationStatus.PENDING)).thenReturn(10L);
        when(photoRepository.countByEventIdAndStatus(1, ModerationStatus.APPROVED)).thenReturn(80L);
        when(photoRepository.countByEventIdAndStatus(1, ModerationStatus.REJECTED)).thenReturn(5L);
        when(photoRepository.countByEventIdAndDeletedAtIsNotNull(1)).thenReturn(5L);

        EventPhotoStatsDto stats = adminEventService.getStats(1);

        assertThat(stats.total()).isEqualTo(100L);
        assertThat(stats.pending()).isEqualTo(10L);
        assertThat(stats.approved()).isEqualTo(80L);
        assertThat(stats.rejected()).isEqualTo(5L);
        assertThat(stats.deleted()).isEqualTo(5L);
    }

    @Test
    void getStats_eventNotFound_throws404() {
        when(eventRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminEventService.getStats(99))
                .isInstanceOf(CustomErrorException.class);
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
