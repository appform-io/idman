/*
 * Copyright 2021. Santanu Sinha
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package io.appform.idman.server.db.impl;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.appform.idman.server.db.PasswordStore;
import io.appform.idman.server.db.model.StoredPassword;
import io.dropwizard.hibernate.AbstractDAO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.SessionFactory;

import javax.inject.Inject;
import java.util.function.UnaryOperator;

/**
 *
 */
@Slf4j
public class DBPasswordStore extends AbstractDAO<StoredPassword> implements PasswordStore {

    @Inject
    public DBPasswordStore(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public void set(String userId, String password) {
        val hashedPassword = hash(password);
        var pwd = passwordForUser(userId);
        if (null != pwd) {
            pwd.setPassword(hashedPassword);
            pwd.setDeleted(false);
        }
        else {
            pwd = new StoredPassword(userId, hashedPassword);
        }
        persist(pwd);
    }

    @Override
    public boolean update(String userId, String oldPassword, String newPassword) {
        return updatePasswordObject(userId, pwd -> {
            if (verify(oldPassword, pwd) && !oldPassword.equals(newPassword)) {
                pwd.setPassword(hash(newPassword));
                return pwd;
            }
            else {
                log.error("Password mismatch for user: {}", userId);
            }
            return null;
        });
    }

    @Override
    public boolean delete(String userId) {
        return updatePasswordObject(userId, pwd -> {
            pwd.setDeleted(true);
            return pwd;
        });
    }

    @Override
    public boolean match(String userId, String password) {
        var pwd = passwordForUser(userId);
        if (null != pwd && !pwd.isDeleted()) {
            return verify(password, pwd);
        }
        return false;
    }

    private static String hash(String s) {
        return BCrypt.withDefaults().hashToString(12, s.toCharArray());
    }

    private static boolean verify(String input, StoredPassword pwd) {
        return BCrypt.verifyer().verify(input.toCharArray(), pwd.getPassword().toCharArray()).verified;
    }

    private StoredPassword passwordForUser(String userId) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredPassword.class);
        return list(cr.select(root).where(cb.equal(root.get("userId"), userId)))
                .stream()
                .findAny()
                .orElse(null);
    }

    private boolean updatePasswordObject(String userId, UnaryOperator<StoredPassword> handler) {
        var pwd = passwordForUser(userId);
        if (null != pwd) {
            val updated = handler.apply(pwd);
            if (null != updated) {
                persist(updated);
                return true;
            }
            else {
                log.error("No valid password object returned. No thing will happen.");
            }
        }
        else {
            log.error("No password data found for user: {}", userId);
        }
        return false;
    }
}
