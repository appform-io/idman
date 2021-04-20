package io.appform.idman.model;

import lombok.Value;

/**
 * High level representation of a user
 */
@Value
public class User {
    String id;
    String name;
    UserType userType;
    AuthMode authMode;
}
