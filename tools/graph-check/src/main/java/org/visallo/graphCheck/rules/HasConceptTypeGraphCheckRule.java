package org.visallo.graphCheck.rules;

import org.vertexium.Edge;
import org.vertexium.Element;
import org.vertexium.Vertex;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.graphCheck.DefaultGraphCheckRule;
import org.visallo.graphCheck.GraphCheckContext;

public class HasConceptTypeGraphCheckRule extends DefaultGraphCheckRule {
    @Override
    public void visitVertex(GraphCheckContext ctx, Vertex vertex) {
        hasConceptType(ctx, vertex);
    }

    @Override
    public void visitEdge(GraphCheckContext ctx, Edge edge) {
        hasConceptType(ctx, edge);
    }

    private void hasConceptType (GraphCheckContext ctx, Element element) {
        if (!VisalloProperties.CONCEPT_TYPE.hasConceptType(element)) {
            ctx.reportError(this, element, "Missing \"%s\"", VisalloProperties.CONCEPT_TYPE.getPropertyName());
        }
    }
}
