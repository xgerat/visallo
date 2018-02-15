package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.IOUtils;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.web.VisalloResponse;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;
import org.visallo.webster.annotations.Required;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class VertexGetPropertyValue implements ParameterizedHandler {
    private static final Pattern RANGE_PATTERN = Pattern.compile("bytes=([0-9]*)-([0-9]*)");
    private static final long UNKNOWN_SPV_LENGTH_PARTIAL_CHUNK_SIZE = 100 * 1024;
    private Graph graph;

    @Inject
    public VertexGetPropertyValue(
            final Graph graph
    ) {
        this.graph = graph;
    }

    @Handle
    public void handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Required(name = "propertyKey") String propertyKey,
            @Required(name = "propertyName") String propertyName,
            @Optional(name = "Range") String range,
            @Optional(name = "download", defaultValue = "false") boolean download,
            @Optional(name = "playback", defaultValue = "false") boolean playback,
            Authorizations authorizations,
            VisalloResponse response
    ) throws Exception {
        PlaybackOptions playbackOptions = new PlaybackOptions();
        playbackOptions.range = range;
        playbackOptions.download = download;
        playbackOptions.playback = playback;

        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            throw new VisalloResourceNotFoundException(String.format("vertex %s not found", graphVertexId));
        }

        Property property = vertex.getProperty(propertyKey, propertyName);
        if (property == null) {
            throw new VisalloResourceNotFoundException(String.format("property %s:%s not found on vertex %s", propertyKey, propertyName, vertex.getId()));
        }

        String fileName = VisalloProperties.FILE_NAME.getOnlyPropertyValue(vertex);

        String mimeType = getMimeType(property);
        if (mimeType != null) {
            response.setContentType(mimeType);
        }

        setFileNameHeaders(response, fileName, playbackOptions);

        Long totalLength;
        InputStream in;
        if (property.getValue() instanceof StreamingPropertyValue) {
            StreamingPropertyValue streamingPropertyValue = (StreamingPropertyValue) property.getValue();
            in = streamingPropertyValue.getInputStream();
            totalLength = streamingPropertyValue.getLength();
        } else {
            byte[] value = property.getValue().toString().getBytes();
            in = new ByteArrayInputStream(value);
            totalLength = (long) value.length;
        }

        try {
            if (playbackOptions.playback) {
                handlePartialPlayback(response, in, totalLength, playbackOptions);
            } else {
                handleFullPlayback(response, in);
            }
        } finally {
            in.close();
        }
    }

    private void setFileNameHeaders(VisalloResponse response, String fileName, PlaybackOptions playbackOptions) {
        if (playbackOptions.download) {
            response.addHeader("Content-Disposition", "attachment; filename=" + fileName);
        } else {
            response.addHeader("Content-Disposition", "inline; filename=" + fileName);
        }
    }

    private void handleFullPlayback(VisalloResponse response, InputStream in) throws IOException {
        try (OutputStream out = response.getOutputStream()) {
            IOUtils.copy(in, out);
        }
    }

    private void handlePartialPlayback(VisalloResponse response, InputStream in, Long totalLength, PlaybackOptions playbackOptions) throws IOException {
        long partialStart = 0;
        Long partialEnd = null;

        if (playbackOptions.range != null) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

            Matcher m = RANGE_PATTERN.matcher(playbackOptions.range);
            if (m.matches()) {
                partialStart = Long.parseLong(m.group(1));
                if (m.group(2).length() > 0) {
                    partialEnd = Long.parseLong(m.group(2));
                }
            }
        }

        if (partialEnd == null) {
            if (totalLength == null) {
                partialEnd = partialStart + UNKNOWN_SPV_LENGTH_PARTIAL_CHUNK_SIZE;
            } else {
                partialEnd = totalLength;
            }
        }

        if (totalLength != null) {
            // Ensure that the last byte position is less than the instance-length
            partialEnd = Math.min(partialEnd, totalLength - 1);
        }
        Long partialLength = totalLength;

        if (playbackOptions.range != null) {
            partialLength = partialEnd - partialStart + 1;
            response.addHeader("Content-Range", String.format("bytes %d-%d/%s", partialStart, partialEnd, totalLength == null ? "*" : totalLength));
            if (partialStart > 0) {
                in.skip(partialStart);
            }
        }

        try (OutputStream out = response.getOutputStream()) {
            if (partialLength != null) {
                response.addHeader("Content-Length", "" + partialLength);
                IOUtils.copyLarge(in, out, partialStart, partialLength);
            } else {
                IOUtils.copyLarge(in, out);
            }
        }
        response.flushBuffer();
    }

    private String getMimeType(Property property) {
        String mimeType = VisalloProperties.MIME_TYPE_METADATA.getMetadataValue(property.getMetadata(), null);
        if (mimeType != null) {
            return mimeType;
        }
        return null;
    }

    public class PlaybackOptions {
        public String range;
        public boolean download;
        public boolean playback;
    }
}
