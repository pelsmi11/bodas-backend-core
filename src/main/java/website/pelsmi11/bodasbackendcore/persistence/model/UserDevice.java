package website.pelsmi11.bodasbackendcore.persistence.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_devices")
public class UserDevice {
    @Id
    @Column(name = "guest_uuid", nullable = false)
    private UUID guestUuid;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "last_active")
    private OffsetDateTime lastActive;

    @ColumnDefault("false")
    @Column(name = "blocked")
    private Boolean blocked;

    @Column(name = "blocked_at")
    private OffsetDateTime blockedAt;

    @OneToMany(mappedBy = "device")
    private Set<Photo> photos = new LinkedHashSet<>();

}
