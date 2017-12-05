package org.visallo.core.model.properties.types;

import org.vertexium.Element;
import org.visallo.core.model.graph.ElementUpdateContext;

import java.util.Date;

/**
 * A VisalloProperty that converts Dates to an appropriate value for
 * storage in Vertexium.
 */
public class DateVisalloProperty extends IdentityVisalloProperty<Date> {
    public DateVisalloProperty(String key) {
        super(key);
    }

    /**
     * Updates the element with the new property value if the property value is newer than the existing property value
     * or the update does not have an existing element (for example a new element or a blind write mutation)
     */
    public <T extends Element> void updatePropertyIfValueIsNewer(
            ElementUpdateContext<T> ctx,
            String propertyKey,
            Date newValue,
            PropertyMetadata metadata,
            Long timestamp
    ) {
        if (isDateNewer(ctx.getElement(), propertyKey, newValue)) {
            updateProperty(ctx, propertyKey, newValue, metadata, timestamp);
        }
    }

    /**
     * Updates the element with the new property value if the property value is newer than the existing property value
     * or the update does not have an existing element (for example a new element or a blind write mutation)
     */
    public <T extends Element> void updatePropertyIfValueIsNewer(
            ElementUpdateContext<T> ctx,
            String propertyKey,
            Date newValue,
            PropertyMetadata metadata
    ) {
        if (isDateNewer(ctx.getElement(), propertyKey, newValue)) {
            updateProperty(ctx, propertyKey, newValue, metadata);
        }
    }

    /**
     * Updates the element with the new property value if the property value is older than the existing property value
     * or the update does not have an existing element (for example a new element or a blind write mutation)
     */
    public <T extends Element> void updatePropertyIfValueIsOlder(
            ElementUpdateContext<T> ctx,
            String propertyKey,
            Date newValue,
            PropertyMetadata metadata,
            Long timestamp
    ) {
        if (isDateOlder(ctx.getElement(), propertyKey, newValue)) {
            updateProperty(ctx, propertyKey, newValue, metadata, timestamp);
        }
    }

    /**
     * Updates the element with the new property value if the property value is older than the existing property value
     * or the update does not have an existing element (for example a new element or a blind write mutation)
     */
    public <T extends Element> void updatePropertyIfValueIsOlder(
            ElementUpdateContext<T> ctx,
            String propertyKey,
            Date newValue,
            PropertyMetadata metadata
    ) {
        if (isDateOlder(ctx.getElement(), propertyKey, newValue)) {
            updateProperty(ctx, propertyKey, newValue, metadata);
        }
    }

    private <T extends Element> boolean isDateNewer(T element, String propertyKey, Date newValue) {
        if (element == null) {
            return true;
        }
        Date existingValue = getPropertyValue(element, propertyKey);
        if (existingValue == null) {
            return true;
        }
        return existingValue.compareTo(newValue) < 0;
    }

    private <T extends Element> boolean isDateOlder(T element, String propertyKey, Date newValue) {
        if (element == null) {
            return true;
        }
        Date existingValue = getPropertyValue(element, propertyKey);
        if (existingValue == null) {
            return true;
        }
        return existingValue.compareTo(newValue) > 0;
    }
}
