package io.appform.idman.server.db.impl;

import io.appform.idman.server.db.FieldNames;
import io.appform.idman.server.db.ServiceStore;
import io.appform.idman.server.db.model.StoredService;
import io.appform.idman.server.utils.Utils;
import io.dropwizard.hibernate.AbstractDAO;
import lombok.val;
import lombok.var;
import org.hibernate.SessionFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 *
 */
public class DBServiceStore extends AbstractDAO<StoredService> implements ServiceStore {

    @Inject
    public DBServiceStore(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Optional<StoredService> create(String name, String description, String callbackPrefix) {
        val id = Utils.readableId(name);
        var service = get(id).orElse(null);
        val secret = UUID.randomUUID().toString();
        if (null == service) {
            service = new StoredService(id, name, description, callbackPrefix, secret);
        }
        else {
            service.setDescription(description);
            service.setCallbackPrefix(callbackPrefix);
            service.setSecret(secret);
            service.setDeleted(false);
        }
        return Optional.of(persist(service));
    }

    @Override
    public Optional<StoredService> get(String serviceId) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredService.class);
        return list(cr.select(root).where(cb.equal(root.get("serviceId"), serviceId)))
                .stream()
                .findAny();
    }

    @Override
    public Optional<StoredService> updateDescription(String serviceId, String description) {
        return updateService(serviceId, service -> service.setDescription(description));
    }

    @Override
    public Optional<StoredService> updateCallbackPrefix(String serviceId, String callbackPrefix) {
        return updateService(serviceId, service -> service.setCallbackPrefix(callbackPrefix));
    }

    @Override
    public Optional<StoredService> updateSecret(String serviceId) {
        return updateService(serviceId, service -> service.setSecret(UUID.randomUUID().toString()));
    }

    @Override
    public boolean delete(String serviceId) {
        val service = get(serviceId).orElse(null);
        if (null == service) {
            return false;
        }
        service.setDeleted(true);
        return persist(service).isDeleted();
    }

    @Override
    public List<StoredService> get(Collection<String> serviceIds) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredService.class);
        val query = cr.select(root).orderBy(cb.asc(root.get("name")));
        return list(query.where(cb.and(
                cb.equal(root.get(FieldNames.DELETED), false),
                root.get(FieldNames.SERVICE_ID).in(serviceIds))));

    }

    @Override
    public List<StoredService> list(boolean includeDeleted) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredService.class);
        val query = cr.select(root).orderBy(cb.asc(root.get("name")));
        if (!includeDeleted) {
            list(query.where(cb.equal(root.get("deleted"), false)));
        }
        return list(query);
    }

    private Optional<StoredService> updateService(String serviceId, Consumer<StoredService> handler) {
        val service = get(serviceId).orElse(null);
        if (null == service) {
            return Optional.empty();
        }
        handler.accept(service);
        return Optional.of(persist(service));
    }

}
