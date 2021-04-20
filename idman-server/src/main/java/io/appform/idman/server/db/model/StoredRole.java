package io.appform.idman.server.db.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.SQLDelete;

import javax.persistence.*;
import java.util.Date;

/**
 *
 */
@Entity
@Table(name = "roles", indexes = {
        @Index(name = "idx_service_role", columnList = "service_id, role_id")
})
@SQLDelete(sql="UPDATE service_permissions SET deleted = '1' WHERE id = ?")
@Data
@NoArgsConstructor
public class StoredRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "role_id", unique = true, nullable = false)
    private String roleId;

    @Column(name = "service_id", nullable = false)
    private String serviceId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "description", nullable = false)
    private String description;

    @Column
    private boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    public StoredRole(String roleId, String serviceId, String displayName, String description) {
        this.roleId = roleId;
        this.serviceId = serviceId;
        this.displayName = displayName;
        this.description = description;
    }
}
