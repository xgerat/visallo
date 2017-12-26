package org.visallo.web;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.visallo.webster.HandlerChain;
import org.visallo.webster.RequestResponseHandler;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.mock;

@RunWith(JUnit4.class)
public class SessionProhibitionFilterTest {
    private SessionProhibitionFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @Before
    public void setup() {
        filter = new SessionProhibitionFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetSessionIsProhibited() throws Exception {
        RequestResponseHandler nextHandler = (request, response, chain) -> {
            request.getSession();
        };
        filter.doFilter(request, response, chain(nextHandler));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetSessionWithArgIsProhibited() throws Exception {
        RequestResponseHandler nextHandler = (request, response, chain) -> {
            request.getSession(true);
        };
        filter.doFilter(request, response, chain(nextHandler));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetRequestSessionIdIsProhibited() throws Exception {
        RequestResponseHandler nextHandler = (request, response, chain) -> {
            request.getRequestedSessionId();
        };
        filter.doFilter(request, response, chain(nextHandler));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIsRequestedSessionIdValidIsProhibited() throws Exception {
        RequestResponseHandler nextHandler = (request, response, chain) -> {
            request.isRequestedSessionIdValid();
        };
        filter.doFilter(request, response, chain(nextHandler));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIsRequestedSessionIdFromCookieIsProhibited() throws Exception {
        RequestResponseHandler nextHandler = (request, response, chain) -> {
            request.isRequestedSessionIdFromCookie();
        };
        filter.doFilter(request, response, chain(nextHandler));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIsRequestedSessionIdFromUrlIsProhibited() throws Exception {
        RequestResponseHandler nextHandler = (request, response, chain) -> {
            request.isRequestedSessionIdFromUrl();
        };
        filter.doFilter(request, response, chain(nextHandler));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIsRequestedSessionIdFromURLIsProhibited() throws Exception {
        RequestResponseHandler nextHandler = (request, response, chain) -> {
            request.isRequestedSessionIdFromURL();
        };
        filter.doFilter(request, response, chain(nextHandler));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testChangeSessionIdIsProhibited() throws Exception {
        RequestResponseHandler nextHandler = (request, response, chain) -> {
            request.changeSessionId();
        };
        filter.doFilter(request, response, chain(nextHandler));
    }

    public FilterChain chain(RequestResponseHandler handler) {
        HandlerChain handlerChain = new HandlerChain(new RequestResponseHandler[] { handler });
        return (request, response) -> {
            try {
                handler.handle((HttpServletRequest)request, (HttpServletResponse)response, handlerChain);
            } catch (UnsupportedOperationException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
