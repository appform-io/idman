package io.appform.idman.server.db.impl;

import io.appform.idman.model.AuthMode;
import io.appform.idman.model.UserType;
import io.appform.idman.server.db.AuthState;
import io.appform.idman.server.db.FieldNames;
import io.appform.idman.server.db.UserInfoStore;
import io.appform.idman.server.db.model.StoredUser;
import io.appform.idman.server.db.model.StoredUserAuthState;
import io.dropwizard.hibernate.AbstractDAO;
import lombok.val;
import lombok.var;
import org.hibernate.SessionFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 *
 */
public class DBUserInfoStore extends AbstractDAO<StoredUser> implements UserInfoStore {

    @Inject
    public DBUserInfoStore(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Optional<StoredUser> create(
            String userId,
            String email,
            String name,
            UserType userType,
            AuthMode authMode,
            boolean expire) {
        var user = get(userId).orElse(null);
        val state = expire ? AuthState.EXPIRED : AuthState.ACTIVE;
        if (null == user) {
            user = new StoredUser(userId, email, name, userType);
            user.setAuthState(new StoredUserAuthState(authMode, state, 0, user));
        }
        else {
            user.setName(name);
            user.setUserType(userType);
            user.setDeleted(false);

            val authState = user.getAuthState();
            authState.setAuthMode(authMode);
            authState.setAuthState(state);
            authState.setFailedAuthCount(0);
        }
        return Optional.of(persist(user));
    }

    @Override
    public Optional<StoredUser> get(String userId) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredUser.class);
        return list(cr.select(root).where(cb.equal(root.get(FieldNames.USER_ID), userId)))
                .stream()
                .findAny();
    }

    @Override
    public Optional<StoredUser> getByEmail(String email) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredUser.class);
        return list(cr.select(root).where(cb.equal(root.get("email"), email)))
                .stream()
                .findAny();
    }

    @Override
    public Optional<StoredUser> updateName(String userId, String name) {
        return updateUser(userId, user -> user.setName(name));
    }

    @Override
    public Optional<StoredUser> updateAuthState(
            String userId, Consumer<StoredUserAuthState> handler) {
        return updateUser(userId, user -> handler.accept(user.getAuthState()));
    }


    @Override
    public boolean deleteUser(String userId) {
        return updateUser(userId, user -> user.setDeleted(true))
                .map(StoredUser::isDeleted)
                .orElse(false);
    }

    @Override
    public List<StoredUser> list(boolean includeDeleted) {
        val cr = criteriaQuery();
        val cb = currentSession().getCriteriaBuilder();
        val root = cr.from(StoredUser.class);
        val query = cr.select(root).orderBy(cb.asc(root.get(FieldNames.USER_ID)));
        if (!includeDeleted) {
            return list(query.where(cb.equal(root.get(FieldNames.DELETED), false)));
        }
        return list(query);
    }

    @Override
    public List<StoredUser> get(Collection<String> userIds) {
        val cr = criteriaQuery();
        val cb = currentSession().getCriteriaBuilder();
        val root = cr.from(StoredUser.class);
        val query = cr.select(root).orderBy(cb.asc(root.get(FieldNames.USER_ID)));
        return list(query.where(root.get(FieldNames.USER_ID).in(userIds)));
    }

    private Optional<StoredUser> updateUser(String userId, Consumer<StoredUser> consumer) {
        var user = get(userId).orElse(null);
        if (null == user) {
            return Optional.empty();
        }
        consumer.accept(user);
        return Optional.of(persist(user));
    }

}
