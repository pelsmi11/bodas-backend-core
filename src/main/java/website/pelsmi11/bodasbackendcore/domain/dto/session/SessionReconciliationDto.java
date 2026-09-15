package website.pelsmi11.bodasbackendcore.domain.dto.session;

import lombok.Data;

/** Reconciliation result; rotateGuestUuid=true ONLY in the FR-010 A/B case. */
@Data
public class SessionReconciliationDto {

    private String action;
    private boolean rotateGuestUuid;

    public SessionReconciliationDto(String action, boolean rotateGuestUuid) {
        this.action = action;
        this.rotateGuestUuid = rotateGuestUuid;
    }
}
