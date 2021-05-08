package io.appform.idman.server.db.model;

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
@Table(name = "user_roles",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_service_user", columnNames = {"user_id", "service_id"})
        },
        indexes = {
            @Index(name = "idx_service_for_role", columnList = "service_id"),
            @Index(name = "idx_service_user", columnList = "service_id, user_id"),
        })
@Data
@NoArgsConstructor
public class StoredUserRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "user_id", nullable = false, length = 45)
    private String userId;

    @Column(name = "service_id", nullable = false)
    private String serviceId;

    @Column(name = "role_id", nullable = false)
    private String roleId;

    @Column(name = "assigned_by")
    private String assignedBy;

    @Column
    private boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    public StoredUserRole(String userId, String serviceId, String roleId, String assignedBy) {
        this.userId = userId;
        this.serviceId = serviceId;
        this.roleId = roleId;
        this.assignedBy = assignedBy;
    }
}
