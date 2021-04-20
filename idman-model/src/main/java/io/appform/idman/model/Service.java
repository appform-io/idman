package io.appform.idman.model;

import lombok.Value;

import java.util.Date;

/**
 * A service being provided by the organization
 */
@Value
public class Service {

    /**
     * Unique ID of the service
     */
    String serviceId;

    /**
     * A Human readable name for the service
     */
    String name;

    /**
     * Service description
     */
    String description;

    /**
     * Has this been deleted
     */
    boolean deleted;

    /**
     * Date when service was created
     */
    Date created;

    /**
     * Date when the service was last updated
     */
    Date updated;
}
