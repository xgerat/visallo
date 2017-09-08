package org.visallo.web.initializers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.core.model.notification.SystemNotificationService;

import javax.servlet.ServletContext;

@Singleton
public class SystemNotificationInitializer extends ApplicationBootstrapInitializer {
    private final SystemNotificationService systemNotificationService;

    @Inject
    public SystemNotificationInitializer(SystemNotificationService systemNotificationService) {
        this.systemNotificationService = systemNotificationService;
    }

    @Override
    public void initialize(ServletContext context) {
        this.systemNotificationService.start();
    }
}
