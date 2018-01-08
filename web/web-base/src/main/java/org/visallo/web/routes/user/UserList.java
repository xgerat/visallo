package org.visallo.web.routes.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.core.model.user.UserSessionCounterRepository;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;
import org.vertexium.util.ConvertingIterable;
import org.vertexium.util.FilterIterable;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.model.workspace.WorkspaceUser;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiUsers;
import org.visallo.web.clientapi.model.UserStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static org.vertexium.util.IterableUtils.toList;

@Singleton
public class UserList implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(UserList.class);
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserSessionCounterRepository sessionCountRepository;

    @Inject
    public UserList(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            UserSessionCounterRepository sessionCountRepository
    ) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.sessionCountRepository = sessionCountRepository;
    }

    @Handle
    public ClientApiUsers handle(
            User user,
            @Optional(name = "q") String query,
            @Optional(name = "workspaceId") String workspaceId,
            @Optional(name = "userIds[]") String[] userIds,
            @Optional(name = "status") String status,
            @Optional(name = "skip", defaultValue = "0") int skip,
            @Optional(name = "limit", defaultValue = "100") int limit,
            @Optional(name = "includeSessionCount", defaultValue = "false") boolean includeSessionCount
    ) throws Exception {
        List<User> users;
        if (userIds != null) {
            checkArgument(query == null, "Cannot use userIds[] and q at the same time");
            checkArgument(workspaceId == null, "Cannot use userIds[] and workspaceId at the same time");
            users = new ArrayList<>();
            for (String userId : userIds) {
                User u = userRepository.findById(userId);
                if (u == null) {
                    LOGGER.error("User " + userId + " not found");
                    continue;
                }
                users.add(u);
            }
        } else if (status != null && status.length() > 0) {
            users = toList(userRepository.findByStatus(skip, limit, UserStatus.valueOf(status)));
        } else {
            users = toList(userRepository.find(query));

            if (workspaceId != null) {
                users = toList(getUsersWithWorkspaceAccess(workspaceId, users, user));
            }
        }

        Iterable<String> workspaceIds = getCurrentWorkspaceIds(users);
        Map<String, String> workspaceNames = getWorkspaceNames(workspaceIds, user);

        Map<String, Integer> sessionCounts = null;
        if (includeSessionCount) {
            sessionCounts = getSessionCounts(users);
        }

        return userRepository.toClientApi(users, workspaceNames, sessionCounts);
    }

    private Map<String, Integer> getSessionCounts(Iterable<User> users) {
        Map<String, Integer> counts = new HashMap<>();
        for (User user : users) {
            String userId = user.getUserId();
            int count = sessionCountRepository.getSessionCount(userId);
            counts.put(userId, count);
        }
        return counts;
    }

    private Map<String, String> getWorkspaceNames(Iterable<String> workspaceIds, User user) {
        Map<String, String> result = new HashMap<>();
        for (Workspace workspace : workspaceRepository.findByIds(workspaceIds, user)) {
            if (workspace != null) {
                result.put(workspace.getWorkspaceId(), workspace.getDisplayTitle());
            }
        }
        return result;
    }

    private Iterable<String> getCurrentWorkspaceIds(Iterable<User> users) {
        return new ConvertingIterable<User, String>(users) {
            @Override
            protected String convert(User user) {
                return user.getCurrentWorkspaceId();
            }
        };
    }

    private Iterable<User> getUsersWithWorkspaceAccess(String workspaceId, final Iterable<User> users, User user) {
        final List<WorkspaceUser> usersWithAccess = workspaceRepository.findUsersWithAccess(workspaceId, user);
        return new FilterIterable<User>(users) {
            @Override
            protected boolean isIncluded(User u) {
                return contains(usersWithAccess, u);
            }

            private boolean contains(List<WorkspaceUser> usersWithAccess, User u) {
                for (WorkspaceUser userWithAccess : usersWithAccess) {
                    if (userWithAccess.getUserId().equals(u.getUserId())) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
