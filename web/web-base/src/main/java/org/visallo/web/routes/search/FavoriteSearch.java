package org.visallo.web.routes.search;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiSuccess;

public class FavoriteSearch implements ParameterizedHandler {
    private final SearchRepository searchRepository;

    @Inject
    public FavoriteSearch(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "id") String id,
            User user
    ) throws Exception {
        this.searchRepository.favoriteSearch(id, user);
        return new ClientApiSuccess();
    }
}
