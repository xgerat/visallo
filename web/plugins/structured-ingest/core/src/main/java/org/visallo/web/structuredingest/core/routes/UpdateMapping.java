package org.visallo.web.structuredingest.core.routes;

import com.google.inject.Singleton;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.visallo.core.security.ACLProvider;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.structuredingest.core.StructuredIngestOntology;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import javax.inject.Inject;

@Singleton
public class UpdateMapping implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(Ingest.class);

    private static final String PROPERTY_KEY = "SFMAPPING";

    private final ACLProvider aclProvider;
    private final VisibilityTranslator visibilityTranslator;
    private final Graph graph;

    @Inject
    public UpdateMapping(
            ACLProvider aclProvider,
            VisibilityTranslator visibilityTranslator,
            Graph graph
    ) {
        this.aclProvider = aclProvider;
        this.visibilityTranslator = visibilityTranslator;
        this.graph = graph;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Required(name = "mapping") String mapping,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            throw new VisalloResourceNotFoundException("Could not find vertex:" + graphVertexId);
        }

        aclProvider.checkCanAddOrUpdateProperty(vertex, PROPERTY_KEY, StructuredIngestOntology.MAPPING.getPropertyName(), user, workspaceId);

        ElementMutation<Vertex> m = vertex.prepareMutation();
        Visibility visibility = visibilityTranslator.getDefaultVisibility();

        StructuredIngestOntology.MAPPING.addPropertyValue(m, PROPERTY_KEY, mapping, visibility);

        m.save(authorizations);
        graph.flush();

        return VisalloResponse.SUCCESS;
    }
}
