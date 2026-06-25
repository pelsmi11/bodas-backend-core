package website.pelsmi11.bodasbackendcore.persistence.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 50)
    @NotNull
    @Column(name = "token", nullable = false, length = 50, unique = true)
    private String token;

    @Size(max = 255)
    @NotNull
    @Column(name = "admin_id", nullable = false)
    private String adminId;

    @Column(name = "event_date")
    private OffsetDateTime eventDate;

    @Size(max = 1000)
    @Column(name = "description", length = 1000)
    private String description;

    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private EventStatus status;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @ColumnDefault("true")
    @Column(name = "is_active")
    private Boolean isActive;

    @PrePersist
    void prePersist() {
        if (status == null) status = EventStatus.ACTIVE;
        if (isActive == null) isActive = true;
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }


}