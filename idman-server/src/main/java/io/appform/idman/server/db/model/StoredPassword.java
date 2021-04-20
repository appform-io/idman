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
@Data
@NoArgsConstructor
@Entity
@Table(name = "passwords")
@SQLDelete(sql="UPDATE passwords SET deleted = '1' WHERE id = ?")
public class StoredPassword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "failed_count")
    private int failedCount;

    @Column
    private boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    public StoredPassword(String userId, String password) {
        this.userId = userId;
        this.password = password;
    }
}
