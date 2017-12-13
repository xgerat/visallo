package org.visallo.web.auth;

import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.CurrentUser;

import javax.crypto.SecretKey;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

public class AuthTokenHttpResponse extends HttpServletResponseWrapper {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(AuthTokenHttpResponse.class);

    private final SecretKey macKey;
    private final HttpServletRequest request;
    private final long tokenValidityDurationInMinutes;
    private final AuthToken token;
    private boolean tokenCookieWritten = false;

    public AuthTokenHttpResponse(AuthToken token, HttpServletRequest request, HttpServletResponse response, SecretKey macKey, long tokenValidityDurationInMinutes) {
        super(response);
        this.token = token;
        this.request = request;
        this.macKey = macKey;
        this.tokenValidityDurationInMinutes = tokenValidityDurationInMinutes;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        updateAuthToken();
        return super.getOutputStream();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        updateAuthToken();
        return super.getWriter();
    }

    public void invalidateAuthentication() {
        if (isCommitted()) {
            throw new IllegalStateException("Unable to clear auth token. The response is already committed.");
        }
        Cookie tokenCookie = new Cookie(AuthTokenFilter.TOKEN_COOKIE_NAME, null);
        tokenCookie.setMaxAge(0);
        tokenCookie.setSecure(true);
        tokenCookie.setHttpOnly(true);
        addCookie(tokenCookie);
        tokenCookieWritten = true;
    }

    private void updateAuthToken() throws IOException {
        if (tokenCookieWritten) {
            return;
        }

        if (token != null && !isTokenNearingExpiration(token)) {
            return;
        }

        String userId = CurrentUser.getUserId(request);
        String username = CurrentUser.getUsername(request);

        if (userId != null && username != null) {
            Date tokenExpiration = calculateTokenExpiration();
            AuthToken token = new AuthToken(userId, username, macKey, tokenExpiration);

            try {
                writeAuthTokenCookie(token);
                tokenCookieWritten = true;
            } catch (AuthTokenException e) {
                LOGGER.error("Auth token serialization failed.", e);
                sendError(SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    private void writeAuthTokenCookie(AuthToken token) throws AuthTokenException {
        if (isCommitted()) {
            throw new IllegalStateException("Response committed before auth token cookie written.");
        }
        Cookie tokenCookie = new Cookie(AuthTokenFilter.TOKEN_COOKIE_NAME, token.serialize());
        tokenCookie.setMaxAge((int) tokenValidityDurationInMinutes * 60 - 60); // subtract a minute so that browsers never send an expired token
        tokenCookie.setSecure(true);
        tokenCookie.setHttpOnly(true);
        addCookie(tokenCookie);
    }

    private Date calculateTokenExpiration() {
        return new Date(System.currentTimeMillis() + (tokenValidityDurationInMinutes * 60 * 1000));
    }

    private boolean isTokenNearingExpiration(AuthToken token) {
        // nearing expiration if remaining time is less than half the token validity duration
        return (token.getExpiration().getTime() - System.currentTimeMillis()) < (tokenValidityDurationInMinutes * 60 * 1000 / 2);
    }
}
