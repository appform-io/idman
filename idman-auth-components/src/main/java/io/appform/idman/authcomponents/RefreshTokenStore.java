package io.appform.idman.authcomponents;

import java.util.Date;
import java.util.Optional;

/**
 *
 */
public interface RefreshTokenStore {
    void store(final String userId, final String refreshToken, final Date accessTokenExpiry);
    Optional<String> refreshToken(String userId);

}
