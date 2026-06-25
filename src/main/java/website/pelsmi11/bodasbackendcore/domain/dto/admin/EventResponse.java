package website.pelsmi11.bodasbackendcore.domain.dto.admin;

import website.pelsmi11.bodasbackendcore.persistence.model.EventStatus;

import java.time.OffsetDateTime;

public record EventResponse(
        Integer id,
        String name,
        String token,
        String adminId,
        OffsetDateTime eventDate,
        String description,
        EventStatus status,
        Boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        long photoCount
) {
}
