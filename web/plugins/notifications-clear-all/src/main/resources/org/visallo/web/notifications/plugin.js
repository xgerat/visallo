define(['public/v1/api'], function(visallo){
    'use strict';

    visallo.registry.registerExtension('org.visallo.dashboard.toolbar.item', {
        identifier: 'org-visallo-notification-clear-all',
        canHandle: function (options) {
            return options.extension.identifier === 'org-visallo-web-notifications'
        },
        tooltip: i18n('org.visallo.notification.clearall.hover'),
        icon: '/org/visallo/web/notifications/trash.png',
        action: {
            type: 'event',
            name: 'notification-clear-all'
        }
    });

    $(document).on('notification-clear-all', function(e, data) {
        require(['util/withDataRequest'], function(withDataRequest) {
            withDataRequest.dataRequest('notification', 'list')
                .done(function (notifications) {
                    var allNotifications = notifications.system.active.concat(notifications.user);
                    var data = {
                        notifications: allNotifications,
                        options: {
                            markRead: true,
                            userDismissed: false,
                            immediate: true,
                            animate: false
                        }
                    };
                    $(document).trigger('dismissNotifications', data);
                });
        })
    })
})