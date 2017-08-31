package org.visallo.core.model.workspace.product;

import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;

public interface WorkProductServiceHasElements {
    /**
     * Called when a work product is being deleted.
     */
   void cleanUpElements(Graph graph, Vertex producVertex, Authorizations authorizations);
}
