package org.visallo.web.product.map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.vertexium.*;
import org.visallo.core.model.graph.ElementUpdateContext;
import org.visallo.core.model.graph.GraphUpdateContext;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.workspace.WorkspaceProperties;
import org.visallo.core.model.workspace.product.UpdateProductEdgeOptions;
import org.visallo.core.model.workspace.product.WorkProductEdge;
import org.visallo.core.model.workspace.product.WorkProductServiceHasElementsBase;
import org.visallo.core.model.workspace.product.WorkProductVertex;

import java.util.Map;
import java.util.Set;

@Singleton
public class MapWorkProductService extends WorkProductServiceHasElementsBase<WorkProductVertex, WorkProductEdge> {
    public static final String KIND = "org.visallo.web.product.map.MapWorkProduct";

    @Inject
    public MapWorkProductService(
            AuthorizationRepository authorizationRepository
    ) {
        super(authorizationRepository);
    }

    @Override
    protected WorkProductEdge createWorkProductEdge() {
        return new WorkProductEdge();
    }

    @Override
    protected WorkProductVertex createWorkProductVertex() {
        return new WorkProductVertex();
    }

    @Override
    protected void updateProductEdge(ElementUpdateContext<Edge> elemCtx, UpdateProductEdgeOptions update, Visibility visibility) {
    }

    @Override
    protected void populateVertexWithWorkspaceEdge(Edge propertyVertexEdge, WorkProductVertex vertex) {
    }

    public void updateVertices(
            GraphUpdateContext ctx,
            Vertex productVertex,
            Map<String, UpdateProductEdgeOptions> updateVertices,
            Visibility visibility
    ) {
        if (updateVertices != null) {
            @SuppressWarnings("unchecked")
            Set<String> vertexIds = updateVertices.keySet();
            for (String id : vertexIds) {
                UpdateProductEdgeOptions updateData = updateVertices.get(id);
                String edgeId = getEdgeId(productVertex.getId(), id);
                EdgeBuilderByVertexId edgeBuilder = ctx.getGraph().prepareEdge(
                        edgeId,
                        productVertex.getId(),
                        id,
                        WorkspaceProperties.PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                        visibility
                );
                ctx.update(edgeBuilder, elemCtx -> updateProductEdge(elemCtx, updateData, visibility));
            }
        }
    }

    public void removeVertices(
            GraphUpdateContext ctx,
            Vertex productVertex,
            String[] removeVertices,
            Authorizations authorizations
    ) {
        if (removeVertices != null) {
            for (String id : removeVertices) {
                ctx.getGraph().softDeleteEdge(getEdgeId(productVertex.getId(), id), authorizations);
            }
        }
    }

    @Override
    public String getKind() {
        return KIND;
    }
}
