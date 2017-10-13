package org.visallo.core.model.properties.types;

import org.vertexium.Metadata;
import org.vertexium.Visibility;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PropertyMetadata {
    private static final Visibility DEFAULT_VISIBILITY = new Visibility("");

    private final Date modifiedDate;
    private final User modifiedBy;
    private final Double confidence;
    private final VisibilityJson visibilityJson;
    private final Visibility propertyVisibility;
    private final List<AdditionalMetadataItem> additionalMetadataItems = new ArrayList<>();

    /**
     * @param modifiedBy The user to set as the modifiedBy metadata
     * @param visibilityJson The visibility json to use in the metadata
     * @param propertyVisibility The visibility of the property
     */
    public PropertyMetadata(User modifiedBy, VisibilityJson visibilityJson, Visibility propertyVisibility) {
        this(new Date(), modifiedBy, visibilityJson, propertyVisibility);
    }

    /**
     * @param modifiedDate The date to use as modifiedDate
     * @param modifiedBy The user to set as the modifiedBy metadata
     * @param visibilityJson The visibility json to use in the metadata
     * @param propertyVisibility The visibility of the property
     */
    public PropertyMetadata(Date modifiedDate, User modifiedBy, VisibilityJson visibilityJson, Visibility propertyVisibility) {
        this(modifiedDate, modifiedBy, null, visibilityJson, propertyVisibility);
    }

    /**
     * @param modifiedDate The date to use as modifiedDate
     * @param modifiedBy The user to set as the modifiedBy metadata
     * @param confidence The confidence metadata value
     * @param visibilityJson The visibility json to use in the metadata
     * @param propertyVisibility The visibility of the property
     */
    public PropertyMetadata(
            Date modifiedDate,
            User modifiedBy,
            Double confidence,
            VisibilityJson visibilityJson,
            Visibility propertyVisibility
    ) {
        this.modifiedDate = modifiedDate;
        this.modifiedBy = modifiedBy;
        this.confidence = confidence;
        this.visibilityJson = visibilityJson;
        this.propertyVisibility = propertyVisibility;
    }

    public PropertyMetadata(PropertyMetadata metadata) {
        this(
                metadata.getModifiedDate(),
                metadata.getModifiedBy(),
                metadata.getConfidence(),
                metadata.getVisibilityJson(),
                metadata.getPropertyVisibility()
        );
        for (AdditionalMetadataItem item : metadata.getAdditionalMetadataItems()) {
            add(item.getKey(), item.getValue(), item.getVisibility());
        }
    }

    public Metadata createMetadata() {
        Metadata metadata = new Metadata();
        VisalloProperties.MODIFIED_DATE_METADATA.setMetadata(metadata, modifiedDate, DEFAULT_VISIBILITY);
        VisalloProperties.MODIFIED_BY_METADATA.setMetadata(metadata, modifiedBy.getUserId(), DEFAULT_VISIBILITY);
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, DEFAULT_VISIBILITY);
        if (confidence != null) {
            VisalloProperties.CONFIDENCE_METADATA.setMetadata(metadata, confidence, DEFAULT_VISIBILITY);
        }
        for (AdditionalMetadataItem additionalMetadataItem : additionalMetadataItems) {
            metadata.add(
                    additionalMetadataItem.getKey(),
                    additionalMetadataItem.getValue(),
                    additionalMetadataItem.getVisibility()
            );
        }
        return metadata;
    }

    public User getModifiedBy() {
        return modifiedBy;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public Double getConfidence() {
        return confidence;
    }

    public VisibilityJson getVisibilityJson() {
        return visibilityJson;
    }

    public Visibility getPropertyVisibility() {
        return propertyVisibility;
    }

    public Iterable<AdditionalMetadataItem> getAdditionalMetadataItems() {
        return additionalMetadataItems;
    }

    public void add(String key, Object value, Visibility visibility) {
        additionalMetadataItems.add(new AdditionalMetadataItem(key, value, visibility));
    }

    private static class AdditionalMetadataItem {
        private final String key;
        private final Object value;
        private final Visibility visibility;

        public AdditionalMetadataItem(String key, Object value, Visibility visibility) {
            this.key = key;
            this.value = value;
            this.visibility = visibility;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }

        public Visibility getVisibility() {
            return visibility;
        }
    }
}
