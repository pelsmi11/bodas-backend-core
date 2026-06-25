package website.pelsmi11.bodasbackendcore.domain.dto.admin;

public record EventPhotoStatsDto(
        long total,
        long pending,
        long approved,
        long rejected,
        long deleted
) {
}
