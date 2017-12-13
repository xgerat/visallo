package org.visallo.web.auth;

import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.CurrentUser;

import javax.crypto.SecretKey;
import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.visallo.core.config.Configuration.*;

public class AuthTokenFilter implements Filter {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(AuthTokenFilter.class);

    public static final String TOKEN_COOKIE_NAME = "JWT";
    private static final String DEFAULT_TOKEN_EXPIRATION_IN_MINS = "60";

    private SecretKey tokenSigningKey;
    private long tokenValidityDurationInMinutes;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        tokenValidityDurationInMinutes = Long.parseLong(getOptionalInitParameter(filterConfig,
                AUTH_TOKEN_EXPIRATION_IN_MINS, DEFAULT_TOKEN_EXPIRATION_IN_MINS));

        String keyPassword = getRequiredInitParameter(filterConfig, AUTH_TOKEN_PASSWORD);
        String keySalt = getRequiredInitParameter(filterConfig, AUTH_TOKEN_SALT);

        try {
            tokenSigningKey = AuthToken.generateKey(keyPassword, keySalt);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        doFilter((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, filterChain);
    }

    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        //TODO: request = new NoSessionHttpRequestWrapper(request);

        try {
            AuthToken token = getAuthToken(request);
            AuthTokenHttpResponse authTokenResponse = new AuthTokenHttpResponse(token, request, response, tokenSigningKey, tokenValidityDurationInMinutes);

            if (token != null) {
                if (token.isExpired()) {
                    authTokenResponse.invalidateAuthentication();
                } else {
                    setCurrentUser(request, token);
                }
            }

            chain.doFilter(request, authTokenResponse);
        } catch (Exception ex) {
            LOGGER.warn("Auth token signature verification failed", ex);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Override
    public void destroy() {

    }

    private AuthToken getAuthToken(HttpServletRequest request) throws AuthTokenException {
        Cookie tokenCookie = getTokenCookie(request);
        return tokenCookie != null ? AuthToken.parse(tokenCookie.getValue(), tokenSigningKey) : null;
    }

    private Cookie getTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(AuthTokenFilter.TOKEN_COOKIE_NAME)) {
                return cookie;
            }
        }

        return null;
    }

    private void setCurrentUser(HttpServletRequest request, AuthToken token) {
        checkNotNull(token.getUserId(), "Auth token did not contain the userId");
        checkNotNull(token.getUsername(), "Auth token did not contain the username");
        CurrentUser.set(request, token.getUserId(), token.getUsername());
    }

    private String getRequiredInitParameter(FilterConfig filterConfig, String parameterName) {
        String parameter = filterConfig.getInitParameter(parameterName);
        checkNotNull(parameter, "FilterConfig init parameter '" + parameterName + "' was not set.");
        return parameter;
    }

    private String getOptionalInitParameter(FilterConfig filterConfig, String parameterName, String defaultValue) {
        String parameterValue = filterConfig.getInitParameter(parameterName);
        if (parameterValue == null || parameterValue.trim().length() < 1) {
            return defaultValue;
        }
        return parameterValue;
    }
}
