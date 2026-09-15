package website.pelsmi11.bodasbackendcore.domain.dto.session;

import lombok.Data;

/** Participation registration result for the optional eventToken. */
@Data
public class SessionParticipationDto {

    private String eventToken;
    private boolean joined;
    private boolean alreadyMember;

    public SessionParticipationDto(String eventToken, boolean joined, boolean alreadyMember) {
        this.eventToken = eventToken;
        this.joined = joined;
        this.alreadyMember = alreadyMember;
    }
}
