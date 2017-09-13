package org.visallo.core.model.workspace.product;

import org.visallo.web.clientapi.model.ClientApiObject;

public class GetExtendedDataParams implements ClientApiObject {
    private boolean includeVertices;
    private boolean includeEdges;

    public boolean isIncludeVertices() {
        return includeVertices;
    }

    public void setIncludeVertices(boolean includeVertices) {
        this.includeVertices = includeVertices;
    }

    public boolean isIncludeEdges() {
        return includeEdges;
    }

    public void setIncludeEdges(boolean includeEdges) {
        this.includeEdges = includeEdges;
    }
}
