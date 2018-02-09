package org.visallo.core.model.properties.types;

import org.vertexium.Element;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.web.clientapi.model.ClientApiElement;

public class ConceptTypeSingleValueVisalloProperty extends StringSingleValueVisalloProperty {
    public ConceptTypeSingleValueVisalloProperty(String key) {
        super(key);
    }

    @Override
    public String getPropertyValue(Element element) {
        String propertyValue = super.getPropertyValue(element);
        return propertyValue == null ? VisalloProperties.CONCEPT_TYPE_THING : propertyValue;
    }

    @Override
    public String getPropertyValue(ClientApiElement element) {
        String propertyValue = super.getPropertyValue(element);
        return propertyValue == null ? VisalloProperties.CONCEPT_TYPE_THING : propertyValue;
    }

    public boolean hasConceptType(Element element) {
        String propertyValue = super.getPropertyValue(element);
        return propertyValue != null;
    }
}
