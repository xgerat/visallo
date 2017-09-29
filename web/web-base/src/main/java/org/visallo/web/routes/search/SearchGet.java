package org.visallo.web.routes.search;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiSearch;

public class SearchGet implements ParameterizedHandler {
    private final SearchRepository searchRepository;

    @Inject
    public SearchGet(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    @Handle
    public ClientApiSearch handle(
            @Required(name = "id") String id,
            User user
    ) throws Exception {
        return searchRepository.getSavedSearch(id, user);
    }
}
