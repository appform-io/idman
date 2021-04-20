package io.appform.idman.authbundle.filters;


import com.google.common.base.Strings;
import io.appform.idman.authbundle.IdmanAuthenticationConfig;
import io.appform.idman.authbundle.SessionUser;
import io.appform.idman.authbundle.impl.IdmanAuthenticator;
import io.appform.idman.authbundle.security.ServiceUserPrincipal;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * This filter validates the token
 */
@Priority(Priorities.AUTHENTICATION)
@WebFilter("/*")
@Slf4j
@Singleton
public class UserAuthenticationFilter implements Filter {

    private final IdmanAuthenticationConfig authConfig;
    private final Authenticator<String, ServiceUserPrincipal> authenticator;
    private final Set<String> allowedPaths;

    @Inject
    public UserAuthenticationFilter(
            IdmanAuthenticationConfig authConfig,
            IdmanAuthenticator authenticator) {
        this.authConfig = authConfig;
        this.authenticator = authenticator;
        this.allowedPaths = null != authConfig.getAllowedPaths()
                            ? authConfig.getAllowedPaths()
                            : Collections.emptySet();
    }

    @Override
    public void init(FilterConfig filterConfig) {
        //Nothing to configure here
    }

    @Override
    public void doFilter(
            ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!authConfig.isEnabled()) {
            log.trace("Auth disabled");
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        final String requestURI = URI.create(httpRequest.getRequestURI()).getPath();
        if (allowedPaths.stream().anyMatch(requestURI::startsWith)) {
            chain.doFilter(request, response);
            return;
        }
        val jwt = getTokenFromCookieOrHeader(httpRequest).orElse(null);
        val referrer = httpRequest.getHeader("Referer");
        if (Strings.isNullOrEmpty(jwt)) {
            log.error("No JWT in request. Will redirect to {}", authConfig.getAuthEndpoint());
        }
        else {
            try {
                val principal = authenticator.authenticate(jwt).orElse(null);
                if (null != principal) {
                    SessionUser.put(principal);
                    chain.doFilter(request, response);
                    return;
                }
            }
            catch (AuthenticationException e) {
                log.error("Jwt validation failure: ", e);
            }
        }
        val source = Strings.isNullOrEmpty(referrer)
                     ? requestURI
                     : referrer;
        httpResponse.addCookie(new Cookie("redirection", source));
        httpResponse.sendRedirect(authConfig.getAuthEndpoint());
    }

    @Override
    public void destroy() {

    }

    private Optional<String> getTokenFromCookieOrHeader(HttpServletRequest servletRequest) {
        val tokenFromHeader = getTokenFromHeader(servletRequest);
        return tokenFromHeader.isPresent()
               ? tokenFromHeader
               : getTokenFromCookie(servletRequest);
    }

    private Optional<String> getTokenFromHeader(HttpServletRequest servletRequest) {
        val header = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null) {
            int space = header.indexOf(' ');
            if (space > 0) {
                final String method = header.substring(0, space);
                if ("Bearer".equalsIgnoreCase(method)) {
                    final String rawToken = header.substring(space + 1);
                    return Optional.of(rawToken);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> getTokenFromCookie(HttpServletRequest request) {
        val cookies = request.getCookies();
        if (null != cookies && cookies.length != 0) {
            val token = Arrays.stream(cookies)
                    .filter(cookie -> cookie.getName().equals("idman-token"))
                    .findAny()
                    .orElse(null);
            if (null != token) {
                return Optional.of(token.getValue());
            }
        }
        return Optional.empty();
    }
}
