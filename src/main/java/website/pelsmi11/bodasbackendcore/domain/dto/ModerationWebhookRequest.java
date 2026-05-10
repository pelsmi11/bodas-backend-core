package website.pelsmi11.bodasbackendcore.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload sent by the moderation worker (Lambda) to report the final verdict.
 */
public record ModerationWebhookRequest(
        @NotBlank String s3Key,
        @NotNull Boolean isApproved,
        String reason
) {
}
