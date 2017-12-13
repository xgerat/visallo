package org.visallo.web.auth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.visallo.web.CurrentUser;

import javax.crypto.SecretKey;
import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.visallo.core.config.Configuration.AUTH_TOKEN_PASSWORD;
import static org.visallo.core.config.Configuration.AUTH_TOKEN_SALT;

@RunWith(MockitoJUnitRunner.class)
public class AuthTokenFilterTest {

    public static final String PASSWORD = "password";
    public static final String SALT = "salt";
    public static final String USERID = "userid";
    public static final String USERNAME = "username";

    @Mock
    private FilterConfig filterConfig;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    private AuthTokenFilter filter;


    @Before
    public void before() throws ServletException {
        when(filterConfig.getInitParameter(AUTH_TOKEN_PASSWORD)).thenReturn(PASSWORD);
        when(filterConfig.getInitParameter(AUTH_TOKEN_SALT)).thenReturn(SALT);
        filter = new AuthTokenFilter();
        filter.init(filterConfig);
    }

    @Test
    public void testNoTokenCookiePresentDoesNotSetToken() throws IOException, ServletException {
        when(request.getCookies()).thenReturn(new Cookie[0]);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(eq(request), any(HttpServletResponse.class));
        verify(response, never()).addCookie(any(Cookie.class));
    }

    @Test
    public void testValidInboundTokenSetsCurrentUser() throws Exception {
        AuthToken token = getToken(USERID, USERNAME, new Date(System.currentTimeMillis() + 10000));
        Cookie cookie = getTokenCookie(token);
        when(request.getCookies()).thenReturn(new Cookie[] { cookie });
        filter.doFilter(request, response, chain);
        verify(request).setAttribute(CurrentUser.STRING_USERID_ATTRIBUTE_NAME, token.getUserId());
        verify(request).setAttribute(CurrentUser.STRING_USERNAME_ATTRIBUTE_NAME, token.getUsername());
        verify(chain).doFilter(eq(request), any(HttpServletResponse.class));
    }

    @Test
    public void testExpiredTokenDoesNotSetCurrentUser() throws Exception {
        AuthToken token = getToken(USERID, USERNAME, new Date(System.currentTimeMillis() - 10000));
        Cookie cookie = getTokenCookie(token);
        when(request.getCookies()).thenReturn(new Cookie[] { cookie });
        filter.doFilter(request, response, chain);
        verify(request, never()).setAttribute(CurrentUser.STRING_USERID_ATTRIBUTE_NAME, token.getUserId());
        verify(request, never()).setAttribute(CurrentUser.STRING_USERNAME_ATTRIBUTE_NAME, token.getUsername());
        verify(chain).doFilter(eq(request), any(HttpServletResponse.class));
    }

    @Test
    public void testExpiredTokenRemovesTokenCookie() throws Exception {
        AuthToken token = getToken(USERID, USERNAME, new Date(System.currentTimeMillis() - 10000));
        Cookie cookie = getTokenCookie(token);
        when(request.getCookies()).thenReturn(new Cookie[] { cookie });
        when(response.isCommitted()).thenReturn(false);
        filter.doFilter(request, response, chain);
        verify(response).addCookie(argThat(new ArgumentMatcher<Cookie>() {
            @Override
            public boolean matches(Object c) {
                Cookie right = (Cookie) c;
                return right.getName().equals(cookie.getName()) && right.getMaxAge() == 0;
            }
        }));
        verify(chain).doFilter(eq(request), any(HttpServletResponse.class));
    }

    @Test
    public void testCurrentUserSetCausesTokenCookieToBeSet() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[0]);
        when(request.getAttribute(CurrentUser.STRING_USERID_ATTRIBUTE_NAME)).thenReturn(USERID);
        when(request.getAttribute(CurrentUser.STRING_USERNAME_ATTRIBUTE_NAME)).thenReturn(USERNAME);
        when(response.isCommitted()).thenReturn(false);

        FilterChain testChain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                response.getWriter();
            }
        };

        filter.doFilter(request, response, testChain);

        verify(response).addCookie(argThat(new ArgumentMatcher<Cookie>() {
            @Override
            public boolean matches(Object c) {
                try {
                    Cookie cookie = (Cookie) c;
                    SecretKey key = AuthToken.generateKey(PASSWORD, SALT);
                    AuthToken token = AuthToken.parse(cookie.getValue(), key);
                    return token.getUserId().equals(USERID) && token.getUsername().equals(USERNAME);
                } catch (Exception e) {
                    fail("token signing failed: " + e);
                }
                return false;
            }
        }));
    }

    @Test
    public void testTokenCookieCloseToExpirationGetsReset() throws Exception {
        AuthToken token = getToken(USERID, USERNAME, new Date(System.currentTimeMillis() + 60000));
        Cookie cookie = getTokenCookie(token);
        when(request.getCookies()).thenReturn(new Cookie[] { cookie });
        when(request.getAttribute(CurrentUser.STRING_USERID_ATTRIBUTE_NAME)).thenReturn(USERID);
        when(request.getAttribute(CurrentUser.STRING_USERNAME_ATTRIBUTE_NAME)).thenReturn(USERNAME);
        when(response.isCommitted()).thenReturn(false);

        FilterChain testChain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                response.getWriter();
            }
        };

        filter.doFilter(request, response, testChain);

        verify(response).addCookie(argThat(new ArgumentMatcher<Cookie>() {
            @Override
            public boolean matches(Object c) {
                try {
                    Cookie cookie = (Cookie) c;
                    SecretKey key = AuthToken.generateKey(PASSWORD, SALT);
                    AuthToken token = AuthToken.parse(cookie.getValue(), key);
                    return token.getUserId().equals(USERID) && token.getUsername().equals(USERNAME);
                } catch (Exception e) {
                    fail("token signing failed: " + e);
                }
                return false;
            }
        }));
    }

    @Test
    public void testTokenSignatureFailureSendsError() throws Exception {
        AuthToken token = getToken(USERID, USERNAME, new Date(System.currentTimeMillis() + 10000));
        Cookie cookie = getTokenCookie(token.serialize() + "a", token.getExpiration());
        when(request.getCookies()).thenReturn(new Cookie[] { cookie });
        filter.doFilter(request, response, chain);
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(request, never()).setAttribute(CurrentUser.STRING_USERID_ATTRIBUTE_NAME, token.getUserId());
        verify(request, never()).setAttribute(CurrentUser.STRING_USERNAME_ATTRIBUTE_NAME, token.getUsername());
        verify(chain, never()).doFilter(eq(request), any(HttpServletResponse.class));
    }

    private AuthToken getToken(String userid, String username, Date expiration) throws InvalidKeySpecException, NoSuchAlgorithmException {
        SecretKey key = AuthToken.generateKey(PASSWORD, SALT);
        return new AuthToken(userid, username, key, expiration);
    }

    private Cookie getTokenCookie(AuthToken token) throws AuthTokenException {
        return getTokenCookie(token.serialize(), token.getExpiration());
    }

    private Cookie getTokenCookie(String value, Date expiration) {
        Cookie cookie = new Cookie(AuthTokenFilter.TOKEN_COOKIE_NAME, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge((int) ((expiration.getTime() - System.currentTimeMillis()) / 1000));
        return cookie;
    }
}
