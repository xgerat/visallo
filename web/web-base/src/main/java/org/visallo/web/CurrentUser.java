package org.visallo.web;

import org.slf4j.MDC;
import org.visallo.web.util.RemoteAddressUtil;

import javax.servlet.http.HttpServletRequest;

public class CurrentUser {
    public static final String STRING_USERID_ATTRIBUTE_NAME = "userid";
    public static final String STRING_USERNAME_ATTRIBUTE_NAME = "username";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_USER_NAME = "userName";
    private static final String MDC_CLIENT_IP_ADDRESS = "clientIpAddress";

    public static void set(HttpServletRequest request, String userId, String userName) {
        request.setAttribute(CurrentUser.STRING_USERID_ATTRIBUTE_NAME, userId);
        request.setAttribute(CurrentUser.STRING_USERNAME_ATTRIBUTE_NAME, userName);
    }

    public static String getUserId(HttpServletRequest request) {
        return (String) request.getAttribute(CurrentUser.STRING_USERID_ATTRIBUTE_NAME);
    }

    public static String getUsername(HttpServletRequest request) {
        return (String) request.getAttribute(CurrentUser.STRING_USERNAME_ATTRIBUTE_NAME);
    }

    public static void clearUserFromLogMappedDiagnosticContexts() {
        MDC.remove(MDC_USER_ID);
        MDC.remove(MDC_USER_NAME);
        MDC.remove(MDC_CLIENT_IP_ADDRESS);
    }

    public static void setUserInLogMappedDiagnosticContexts(HttpServletRequest request) {
        String userId = CurrentUser.getUserId(request);
        if (userId != null) {
            MDC.put(MDC_USER_ID, userId);
        }
        String userName = CurrentUser.getUsername(request);
        if (userName != null) {
            MDC.put(MDC_USER_NAME, userName);
        }

        MDC.put(MDC_CLIENT_IP_ADDRESS, RemoteAddressUtil.getClientIpAddr(request));
    }
}
