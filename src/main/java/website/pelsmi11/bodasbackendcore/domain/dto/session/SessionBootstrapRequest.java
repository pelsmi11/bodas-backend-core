package website.pelsmi11.bodasbackendcore.domain.dto.session;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Request body for POST /api/v1/auth/session/bootstrap (public field name is guestUuid). */
@Data
public class SessionBootstrapRequest {

    @NotNull
    private UUID guestUuid;

    /** Optional event token: registers participation idempotently when valid. */
    private String eventToken;
}
