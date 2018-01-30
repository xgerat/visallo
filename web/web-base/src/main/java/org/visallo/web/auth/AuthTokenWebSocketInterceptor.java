package org.visallo.web.auth;

import org.apache.commons.lang.StringUtils;
import org.atmosphere.cpr.*;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.CurrentUser;

import javax.servlet.http.HttpServletRequest;

import static com.google.common.base.Preconditions.checkNotNull;

public class AuthTokenWebSocketInterceptor implements AtmosphereInterceptor {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(AuthTokenWebSocketInterceptor.class);

    private int tokenExpirationToleranceInSeconds;
    private UserRepository userRepository;
    private AuthTokenRepository authTokenRepository;

    @Override
    public void configure(AtmosphereConfig config) {
        tokenExpirationToleranceInSeconds = config.getInitParameter(Configuration.AUTH_TOKEN_EXPIRATION_TOLERANCE_IN_SECS, 0);
        userRepository = InjectHelper.getInstance(UserRepository.class);
        authTokenRepository = InjectHelper.getInstance(AuthTokenRepository.class);
    }

    @Override
    public Action inspect(AtmosphereResource resource) {
        try {
            AtmosphereRequest request = resource.getRequest();
            AuthToken token = getAuthToken(request);

            if (token != null && token.isValid(tokenExpirationToleranceInSeconds) && token.getUsage() == AuthTokenUse.WEB) {
                setCurrentUser(request, token);
            }
        } catch (AuthTokenException e) {
            LOGGER.warn("Auth token verification failed", e);
            return Action.CANCELLED;
        }

        return Action.CONTINUE;
    }

    @Override
    public void postInspect(AtmosphereResource resource) {
        // noop
    }

    @Override
    public void destroy() {
        // noop
    }

    private AuthToken getAuthToken(AtmosphereRequest request) throws AuthTokenException {
        String cookieString = request.getHeader("cookie");

        if (cookieString != null) {
            int tokenCookieIndex = cookieString.indexOf(AuthTokenFilter.TOKEN_COOKIE_NAME);
            if (tokenCookieIndex > -1) {
                int equalsSeperatorIndex = cookieString.indexOf("=", tokenCookieIndex);
                int cookieSeparatorIndex = cookieString.indexOf(";", equalsSeperatorIndex);
                if (cookieSeparatorIndex < 0) {
                    cookieSeparatorIndex = cookieString.length();
                }
                String tokenString = cookieString.substring(equalsSeperatorIndex + 1, cookieSeparatorIndex).trim();
                if (!StringUtils.isEmpty(tokenString)) {
                    return authTokenRepository.parse(tokenString);
                }
            }
        }

        return null;
    }

    private void setCurrentUser(HttpServletRequest request, AuthToken token) {
        checkNotNull(token.getUserId(), "Auth token did not contain the userId");
        User user = userRepository.findById(token.getUserId());
        CurrentUser.set(request, user, token);
    }
}
