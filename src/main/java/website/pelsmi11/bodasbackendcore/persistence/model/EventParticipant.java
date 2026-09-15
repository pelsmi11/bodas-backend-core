package website.pelsmi11.bodasbackendcore.persistence.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * Persistent guest-to-event membership (V4 table {@code event_participants},
 * already applied — NO new migration). Composite PK (event_id, user_id);
 * many-to-many; confers NO administrative rights (FR-011).
 */
@Entity
@Table(name = "event_participants")
@IdClass(EventParticipantId.class)
public class EventParticipant {

    @Id
    @Column(name = "event_id", insertable = false, updatable = false)
    private Integer eventId;

    @Id
    @Column(name = "user_id", insertable = false, updatable = false)
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("eventId")
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "last_accessed_at", nullable = false)
    private OffsetDateTime lastAccessedAt;

    public EventParticipant() {
    }

    public EventParticipant(Event event, User user, OffsetDateTime joinedAt, OffsetDateTime lastAccessedAt) {
        this.event = event;
        this.user = user;
        this.eventId = event != null ? event.getId() : null;
        this.userId = user != null ? user.getId() : null;
        this.joinedAt = joinedAt;
        this.lastAccessedAt = lastAccessedAt;
    }

    public Integer getEventId() {
        return eventId;
    }

    public Integer getUserId() {
        return userId;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(OffsetDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public OffsetDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(OffsetDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
}
