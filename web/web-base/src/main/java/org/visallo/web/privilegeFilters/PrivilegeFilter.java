package org.visallo.web.privilegeFilters;

import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.user.User;
import org.visallo.web.CurrentUser;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.webster.HandlerChain;
import org.visallo.webster.RequestResponseHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

public class PrivilegeFilter implements RequestResponseHandler {
    private final Set<String> requiredPrivileges;
    private final PrivilegeRepository privilegeRepository;

    protected PrivilegeFilter(
            Set<String> requiredPrivileges,
            PrivilegeRepository privilegeRepository
    ) {
        this.requiredPrivileges = requiredPrivileges;
        this.privilegeRepository = privilegeRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = CurrentUser.get(request);
        if (!privilegeRepository.hasAllPrivileges(user, requiredPrivileges)) {
            throw new VisalloAccessDeniedException(
                    "You do not have the required privileges: " + Privilege.toString(requiredPrivileges),
                    user,
                    "privileges"
            );
        }
        chain.next(request, response);
    }
}
