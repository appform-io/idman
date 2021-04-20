package io.appform.idman.server.db.model;

import io.appform.idman.server.utils.Utils;
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
@Table(
        name = "sessions",
        indexes = {
                @Index(name = "idx_session", columnList = "session_id"),
                @Index(name = "idx_user", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "pk_id_pid", columnNames = {"id", "partition_id"})
        }
)
@Data
@NoArgsConstructor
public class StoredUserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "session_id", unique = true)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false)
    private SessionType type;

    @Column
    private Date expiry;

    @Column(name = "partition_id")
    private int partitionId;

    @Column
    private boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    public StoredUserSession(String sessionId, String userId, SessionType type, Date expiry) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.type = type;
        this.expiry = expiry;
        this.partitionId = Utils.weekOfYear();
    }
}
