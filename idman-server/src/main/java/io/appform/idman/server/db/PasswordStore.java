package io.appform.idman.server.db;

/**
 *
 */
public interface PasswordStore {
    void set(String userId, String password);
    boolean update(String userId, String oldPassword, String newPassword);
    boolean delete(String userId);
    boolean match(String userId, String password);
}
