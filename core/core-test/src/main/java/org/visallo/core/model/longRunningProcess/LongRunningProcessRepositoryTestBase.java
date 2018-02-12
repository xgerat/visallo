package org.visallo.core.model.longRunningProcess;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloInMemoryTestBase;

import java.util.List;

import static org.junit.Assert.*;

public abstract class LongRunningProcessRepositoryTestBase extends VisalloInMemoryTestBase {
    private User user;
    private User systemUser;
    private LongRunningProcessRepository lrpr;

    @Before
    public void before() throws Exception {
        super.before();
        user = getUserRepository().findOrAddUser("user1", "user", "user@user.com", "none");
        systemUser = getUserRepository().getSystemUser();
        lrpr = getLongRunningProcessRepository();
    }

    @Test
    public void testAsNormalUser() {
        LongRunningProcessQueueItem item = new LongRunningProcessQueueItem();
        String id = lrpr.enqueue(item, user, getGraphAuthorizations(user));
        assertNotNull("id should not be null for non-system users", id);

        List<JSONObject> items = lrpr.getLongRunningProcesses(user);
        assertEquals(1, items.size());

        JSONObject itemJson = lrpr.findById(id, user);
        assertNotNull("should find item with id", itemJson);

        lrpr.ack(itemJson);
    }

    @Test
    public void testAsSystemUser() {
        LongRunningProcessQueueItem item = new LongRunningProcessQueueItem();
        String id = lrpr.enqueue(item, systemUser, getGraphAuthorizations(systemUser));
        assertNull("id should be null for system users", id);
    }

    @Test
    public void testSystemUserGetLongRunningProcesses() {
        List<JSONObject> items = lrpr.getLongRunningProcesses(user);
        assertEquals(0, items.size());
    }

    @Test
    public void testSystemUserAck() {
        lrpr.ack(new JSONObject());
    }

    @Test
    public void testSystemUserNak() {
        lrpr.nak(new JSONObject(), new VisalloException("error"));
    }

    @Test
    public void testSystemUserReportProgress() {
        lrpr.reportProgress(new JSONObject(), 1.0, "test");
    }

    @Test
    public void testSystemUserCancel() {
        lrpr.cancel(null, systemUser);
    }

    @Test
    public void testSystemUserDelete() {
        lrpr.delete(null, systemUser);
    }

    public abstract LongRunningProcessRepository getLongRunningProcessRepository();

    protected static class LongRunningProcessQueueItem extends LongRunningProcessQueueItemBase {

    }
}
