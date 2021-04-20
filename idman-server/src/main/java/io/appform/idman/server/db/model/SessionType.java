package io.appform.idman.server.db.model;

/**
 * Type of session
 */
public enum SessionType {
    /**
     * Session is from a login context
     */
    DYNAMIC,

    /**
     * Session is from token auth context
     */
    STATIC
}
