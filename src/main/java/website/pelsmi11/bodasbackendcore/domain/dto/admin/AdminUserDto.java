package website.pelsmi11.bodasbackendcore.domain.dto.admin;

import website.pelsmi11.bodasbackendcore.persistence.model.UserRole;

import java.time.OffsetDateTime;

public record AdminUserDto(
        Integer id,
        String cognitoId,
        String name,
        String email,
        UserRole role,
        OffsetDateTime createdAt,
        OffsetDateTime lastLoginAt,
        long deviceCount,
        long photoCount
) {
}
