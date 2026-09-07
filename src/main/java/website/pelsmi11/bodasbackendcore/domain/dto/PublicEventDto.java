package website.pelsmi11.bodasbackendcore.domain.dto;

import java.time.OffsetDateTime;

/**
 * Public event details exposed to guests who know the event token.
 */
public record PublicEventDto(
        String eventToken,
        String name,
        OffsetDateTime eventDate,
        String description
) {
}
