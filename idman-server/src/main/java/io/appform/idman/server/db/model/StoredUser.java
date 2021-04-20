package io.appform.idman.server.db.model;

import io.appform.idman.model.UserType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.util.Date;

/**
 *
 */
@Entity
@Table(name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user", columnNames = "user_id")
    })
@Data
@NoArgsConstructor
public class StoredUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "user_id", unique = true, nullable = false, length = 45)
    private String userId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 45)
    private UserType userType;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "auth_state_id", referencedColumnName = "id")
    private StoredUserAuthState authState;

    @Column
    private boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;


    public StoredUser(String userId, String email, String name, UserType userType) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.userType = userType;
    }
}
