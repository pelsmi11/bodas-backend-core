package website.pelsmi11.bodasbackendcore.domain.service.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

/**
 * Administrative operations for event lifecycle management.
 */
@Service
public class AdminEventService {

    private final EventRepository eventRepository;
    private final PhotoRepository photoRepository;
    private final EventTokenGenerator tokenGenerator;

    public AdminEventService(
            EventRepository eventRepository,
            PhotoRepository photoRepository,
            EventTokenGenerator tokenGenerator
    ) {
        this.eventRepository = eventRepository;
        this.photoRepository = photoRepository;
        this.tokenGenerator = tokenGenerator;
    }

    @Transactional
    public EventResponse create(EventCreateRequest request, String adminSub) {
        Event event = new Event();
        event.setName(request.getName());
        event.setToken(tokenGenerator.generateUniqueToken());
        event.setAdminId(request.getAdminId() != null && !request.getAdminId().isBlank()
                ? request.getAdminId()
                : adminSub);
        event.setEventDate(request.getEventDate());
        event.setDescription(request.getDescription());
        event.setStatus(EventStatus.ACTIVE);
        event.setIsActive(true);

        Event saved = eventRepository.save(event);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> findAll(String search, EventStatus status, Pageable pageable) {
        Page<Event> page;
        if (status != null && search != null && !search.isBlank()) {
            page = eventRepository.findByNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(search, status, pageable);
        } else if (status != null) {
            page = eventRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else if (search != null && !search.isBlank()) {
            page = eventRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc(search, pageable);
        } else {
            page = eventRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse findById(Integer id) {
        return toResponse(requireEvent(id));
    }

    @Transactional
    public EventResponse update(Integer id, EventUpdateRequest request) {
        Event event = requireEvent(id);
        if (request.getName() != null) event.setName(request.getName());
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getStatus() != null) event.setStatus(parseStatus(request.getStatus()));
        Event saved = eventRepository.save(event);
        return toResponse(saved);
    }

    @Transactional
    public EventTokenRegenerateResponse regenerateToken(Integer id) {
        Event event = requireEvent(id);
        event.setToken(tokenGenerator.generateUniqueToken());
        eventRepository.save(event);
        return new EventTokenRegenerateResponse(event.getToken());
    }

    @Transactional
    public EventResponse setActive(Integer id, boolean active) {
        Event event = requireEvent(id);
        event.setIsActive(active);
        if (!active) {
            event.setStatus(EventStatus.ARCHIVED);
        } else if (event.getStatus() == EventStatus.ARCHIVED) {
            event.setStatus(EventStatus.ACTIVE);
        }
        Event saved = eventRepository.save(event);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Integer id) {
        Event event = requireEvent(id);
        event.setIsActive(false);
        event.setStatus(EventStatus.ARCHIVED);
        eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public EventPhotoStatsDto getStats(Integer eventId) {
        requireEvent(eventId);
        long total = photoRepository.countByEventId(eventId);
        long pending = photoRepository.countByEventIdAndStatus(eventId, ModerationStatus.PENDING);
        long approved = photoRepository.countByEventIdAndStatus(eventId, ModerationStatus.APPROVED);
        long rejected = photoRepository.countByEventIdAndStatus(eventId, ModerationStatus.REJECTED);
        long deleted = photoRepository.countByEventIdAndDeletedAtIsNotNull(eventId);
        return new EventPhotoStatsDto(total, pending, approved, rejected, deleted);
    }

    private Event requireEvent(Integer id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> CustomErrorException.handlerCustomError("Event not found", HttpStatus.NOT_FOUND));
    }

    private EventResponse toResponse(Event event) {
        long photoCount = photoRepository.countByEventId(event.getId());
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getToken(),
                event.getAdminId(),
                event.getEventDate(),
                event.getDescription(),
                event.getStatus(),
                event.getIsActive(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                photoCount
        );
    }

    private EventStatus parseStatus(String status) {
        try {
            return EventStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw CustomErrorException.handlerCustomError("Invalid event status: " + status, HttpStatus.BAD_REQUEST);
        }
    }
}
