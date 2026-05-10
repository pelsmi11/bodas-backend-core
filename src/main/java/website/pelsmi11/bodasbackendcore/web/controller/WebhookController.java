package website.pelsmi11.bodasbackendcore.web.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import website.pelsmi11.bodasbackendcore.domain.dto.ApiResponse;
import website.pelsmi11.bodasbackendcore.domain.dto.ModerationWebhookRequest;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.domain.service.ModerationService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Receives moderation callbacks from Lambda/Webhook integrations.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final ModerationService moderationService;

    @Value("${app.webhook.secret}")
    private String expectedSecret;

    public WebhookController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    /**
     * Validates webhook shared secret and applies moderation verdict.
     */
    @PostMapping("/moderation")
    public ApiResponse<String> handleModerationResult(
            @RequestHeader("X-Webhook-Secret") String providedSecret,
            @Valid @RequestBody ModerationWebhookRequest request
    ) {
        if (!secureEquals(expectedSecret, providedSecret)) {
            throw CustomErrorException.handlerCustomError("Invalid Webhook Secret", HttpStatus.UNAUTHORIZED);
        }

        moderationService.processModerationResult(request);
        return ApiResponse.ok("Moderation processed");
    }

    private boolean secureEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
