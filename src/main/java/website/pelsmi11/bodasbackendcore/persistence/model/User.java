package website.pelsmi11.bodasbackendcore.persistence.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 255)
    @Column(name = "cognito_id")
    private String cognitoId;

    @Size(max = 100)
    @Column(name = "name", length = 100)
    private String name;

    @Size(max = 255)
    @Column(name = "email")
    private String email;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "user")
    private Set<UserDevice> devices = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<Photo> photos = new LinkedHashSet<>();

}
