package org.visallo.web.product.map;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vertexium.*;
import org.visallo.core.model.graph.ElementUpdateContext;
import org.visallo.core.model.graph.GraphUpdateContext;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.workspace.WorkspaceProperties;
import org.visallo.core.model.workspace.product.WorkProductServiceHasElementsBase;
import org.visallo.core.util.JSONUtil;

import java.util.List;

@Singleton
public class MapWorkProductService extends WorkProductServiceHasElementsBase {
    public static final String KIND = "org.visallo.web.product.map.MapWorkProduct";

    @Inject
    public MapWorkProductService(
            AuthorizationRepository authorizationRepository
    ) {
        super(authorizationRepository);
    }

    @Override
    protected void updateProductEdge(ElementUpdateContext<Edge> elemCtx, JSONObject update, Visibility visibility) {
    }

    protected void setEdgeJson(Edge propertyVertexEdge, JSONObject vertex) {
    }

    public void updateVertices(
            GraphUpdateContext ctx,
            Vertex productVertex,
            JSONObject updateVertices,
            Visibility visibility
    ) {
        if (updateVertices != null) {
            @SuppressWarnings("unchecked")
            List<String> vertexIds = Lists.newArrayList(updateVertices.keys());
            for (String id : vertexIds) {
                JSONObject updateData = updateVertices.getJSONObject(id);
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
            JSONArray removeVertices,
            Authorizations authorizations
    ) {
        if (removeVertices != null) {
            JSONUtil.toList(removeVertices)
                    .forEach(id -> ctx.getGraph().softDeleteEdge(getEdgeId(productVertex.getId(), (String) id), authorizations));
        }
    }

    @Override
    public String getKind() {
        return KIND;
    }
}
