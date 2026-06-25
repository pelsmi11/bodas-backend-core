package website.pelsmi11.bodasbackendcore.domain.dto.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminDeviceDto(
        UUID guestUuid,
        Integer userId,
        String userCognitoId,
        OffsetDateTime lastActive,
        Boolean blocked,
        OffsetDateTime blockedAt,
        long photoCount
) {
}
