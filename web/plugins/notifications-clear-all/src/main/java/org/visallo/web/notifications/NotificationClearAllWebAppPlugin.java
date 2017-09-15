package org.visallo.web.notifications;

import com.v5analytics.webster.Handler;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;

import javax.servlet.ServletContext;

@Name("Notifications")
@Description("Add clear all option to notifications")
public class NotificationClearAllWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        app.registerJavaScript("/org/visallo/web/notifications/plugin.js", true);
        app.registerResourceBundle("/org/visallo/web/notifications/messages.properties");
        app.registerFile("/org/visallo/web/notifications/trash.png", "image/png");
    }
}
