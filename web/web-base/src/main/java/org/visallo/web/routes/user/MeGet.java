package org.visallo.web.routes.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiUser;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.handlers.CSRFHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Singleton
public class MeGet implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(MeGet.class);
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public MeGet(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository
    ) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiUser handle(
            HttpServletRequest request,
            HttpServletResponse response,
            User user
    ) throws Exception {
        ClientApiUser userMe = userRepository.toClientApiPrivate(user);
        userMe.setCsrfToken(CSRFHandler.getSavedToken(request, response, true));

        try {
            if (userMe.getCurrentWorkspaceId() != null && userMe.getCurrentWorkspaceId().length() > 0) {
                if (!workspaceRepository.hasReadPermissions(userMe.getCurrentWorkspaceId(), user)) {
                    userMe.setCurrentWorkspaceId(null);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to read user's current workspace %s", user.getCurrentWorkspaceId(), ex);
            userMe.setCurrentWorkspaceId(null);
        }

        if (userMe.getCurrentWorkspaceId() == null) {
            Iterable<Workspace> allWorkspaces = workspaceRepository.findAllForUser(user);
            Workspace workspace = null;
            if (allWorkspaces != null) {
                Optional<Workspace> first = StreamSupport.stream(allWorkspaces.spliterator(), false)
                        .filter(workspace1 -> {
                            String creator = workspaceRepository.getCreatorUserId(workspace1.getWorkspaceId(), user);
                            return creator.equals(user.getUserId());
                        })
                        .sorted(Comparator.comparing(w -> w.getDisplayTitle().toLowerCase()))
                        .findFirst();

                if (first.isPresent()) {
                    workspace = first.get();
                }
            }

            if (workspace == null) {
                workspace = workspaceRepository.add(user);
            }

            userRepository.setCurrentWorkspace(user.getUserId(), workspace.getWorkspaceId());
            userMe.setCurrentWorkspaceId(workspace.getWorkspaceId());
            userMe.setCurrentWorkspaceName(workspace.getDisplayTitle());
        }

        return userMe;
    }
}
