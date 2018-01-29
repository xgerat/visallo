package org.visallo.web.auth;

import org.apache.commons.lang.StringUtils;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.CurrentUser;

import javax.crypto.SecretKey;
import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.visallo.core.config.Configuration.*;

public class AuthTokenFilter implements Filter {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(AuthTokenFilter.class);
    private static final int MIN_AUTH_TOKEN_EXPIRATION_MINS = 1;
    public static final String TOKEN_COOKIE_NAME = "JWT";
    public static final String TOKEN_HTTP_HEADER_NAME = "Authorization";
    public static final String TOKEN_HTTP_HEADER_TYPE = "Bearer";

    private SecretKey tokenSigningKey;
    private long tokenValidityDurationInMinutes;
    private int tokenExpirationToleranceInSeconds;
    private UserRepository userRepository;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        tokenValidityDurationInMinutes = Long.parseLong(
                getRequiredInitParameter(filterConfig, AUTH_TOKEN_EXPIRATION_IN_MINS)
        );
        if (tokenValidityDurationInMinutes < MIN_AUTH_TOKEN_EXPIRATION_MINS) {
            throw new VisalloException("Configuration: " +
                    "'" + AUTH_TOKEN_EXPIRATION_IN_MINS + "' " +
                    "must be at least " + MIN_AUTH_TOKEN_EXPIRATION_MINS + " minute(s)"
            );
        }

        tokenExpirationToleranceInSeconds = Integer.parseInt(
                getRequiredInitParameter(filterConfig, Configuration.AUTH_TOKEN_EXPIRATION_TOLERANCE_IN_SECS)
        );

        String keyPassword = getRequiredInitParameter(filterConfig, AUTH_TOKEN_PASSWORD);
        String keySalt = getRequiredInitParameter(filterConfig, AUTH_TOKEN_SALT);
        userRepository = InjectHelper.getInstance(UserRepository.class);

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
        AuthToken token = getAuthToken(request);
        AuthTokenHttpResponse authTokenResponse = new AuthTokenHttpResponse(token, request, response, tokenSigningKey, tokenValidityDurationInMinutes);

        CurrentUser.unset(request);
        if (token != null) {
            if (!token.isValid(tokenExpirationToleranceInSeconds)) {
                authTokenResponse.invalidateAuthentication();
                if (!token.isVerified()) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            } else {
                User user = userRepository.findById(token.getUserId());
                if (user != null) {
                    CurrentUser.set(request, user, token);
                } else {
                    authTokenResponse.invalidateAuthentication();
                }
            }
        }

        chain.doFilter(request, authTokenResponse);
    }

    @Override
    public void destroy() {

    }

    private AuthToken getAuthToken(HttpServletRequest request) {
        try {
            Cookie tokenCookie = getTokenCookie(request);
            if (tokenCookie != null) {
                AuthToken authToken = AuthToken.parse(tokenCookie.getValue(), tokenSigningKey);
                if (authToken.getUsage() == AuthTokenUse.WEB) {
                    return authToken;
                }
            }

            String authHeader = getTokenHeader(request);
            if (authHeader != null) {
                AuthToken authToken = AuthToken.parse(authHeader, tokenSigningKey);
                if (authToken.getUsage() == AuthTokenUse.API) {
                    return authToken;
                } else {
                    LOGGER.warn("Non API web token passed as request header.");
                }
            }
        } catch (AuthTokenException ate) {
            LOGGER.warn("Failed to parse auth token ", ate);
        }
        return null;
    }

    private String getTokenHeader(HttpServletRequest request) {
        Enumeration<String> headers = request.getHeaders(TOKEN_HTTP_HEADER_NAME);
        while (headers != null && headers.hasMoreElements()) {
            String value = headers.nextElement();
            if (value.toLowerCase().startsWith(TOKEN_HTTP_HEADER_TYPE.toLowerCase())) {
                String authHeaderValue = value.substring(TOKEN_HTTP_HEADER_TYPE.length()).trim();
                int commaIndex = authHeaderValue.indexOf(',');
                if (commaIndex > 0) {
                    authHeaderValue = authHeaderValue.substring(0, commaIndex);
                }
                return authHeaderValue;
            }
        }

        return null;
    }

    private Cookie getTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        Cookie found = null;

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(AuthTokenFilter.TOKEN_COOKIE_NAME)) {
                if (StringUtils.isEmpty(cookie.getValue())) {
                    return null;
                } else {
                    found = cookie;
                }
            }
        }

        return found;
    }

    private String getRequiredInitParameter(FilterConfig filterConfig, String parameterName) {
        String parameter = filterConfig.getInitParameter(parameterName);
        checkNotNull(parameter, "FilterConfig init parameter '" + parameterName + "' was not set.");
        return parameter;
    }
}
