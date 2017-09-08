package org.visallo.web.privilegeFilters;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.web.clientapi.model.Privilege;

@Singleton
public class OntologyAddPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected OntologyAddPrivilegeFilter(final UserRepository userRepository, PrivilegeRepository privilegeRepository) {
        super(Privilege.newSet(Privilege.ONTOLOGY_ADD), userRepository, privilegeRepository);
    }
}
