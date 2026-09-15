package website.pelsmi11.bodasbackendcore.domain.dto.session;

import java.util.List;

import lombok.Data;

/**
 * Normalized bootstrap session: profile, role (from cognito:groups), available
 * events, reconciliation result and participation. Contains NO photos (FR-012a:
 * no personal gallery) and NO guest-provided names/messages (FR-012).
 */
@Data
public class SessionResponse {

    private SessionUserDto user;
    private String role;
    private List<SessionEventDto> events;
    private SessionReconciliationDto reconciliation;
    private SessionParticipationDto participation;
}
