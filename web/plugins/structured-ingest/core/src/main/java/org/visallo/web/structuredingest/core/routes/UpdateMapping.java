package org.visallo.web.structuredingest.core.routes;

import com.google.inject.Singleton;
import org.vertexium.Visibility;
import org.vertexium.mutation.ElementMutation;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.visallo.web.structuredingest.core.StructuredIngestOntology;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;
import org.visallo.webster.annotations.Required;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRepository;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiObject;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.structuredingest.core.model.ClientApiMappingErrors;
import org.visallo.web.structuredingest.core.model.StructuredIngestParser;
import org.visallo.web.structuredingest.core.util.StructuredIngestParserFactory;
import org.visallo.web.structuredingest.core.model.StructuredIngestQueueItem;
import org.visallo.web.structuredingest.core.util.BaseStructuredFileParserHandler;
import org.visallo.web.structuredingest.core.util.GraphBuilderParserHandler;
import org.visallo.web.structuredingest.core.model.ParseOptions;
import org.visallo.web.structuredingest.core.util.ProgressReporter;
import org.visallo.web.structuredingest.core.util.mapping.ParseMapping;
import org.visallo.web.structuredingest.core.worker.StructuredIngestProcessWorker;

import javax.inject.Inject;
import java.io.InputStream;

@Singleton
public class UpdateMapping implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(Ingest.class);

    private static final String PROPERTY_KEY = "SFMAPPING";

    private final LongRunningProcessRepository longRunningProcessRepository;
    private final OntologyRepository ontologyRepository;
    private final PrivilegeRepository privilegeRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceHelper workspaceHelper;
    private final WorkQueueRepository workQueueRepository;
    private final Graph graph;
    private final StructuredIngestParserFactory structuredIngestParserFactory;

    @Inject
    public UpdateMapping(
            LongRunningProcessRepository longRunningProcessRepository,
            OntologyRepository ontologyRepository,
            PrivilegeRepository privilegeRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceHelper workspaceHelper,
            StructuredIngestParserFactory structuredIngestParserFactory,
            WorkQueueRepository workQueueRepository,
            VisibilityTranslator visibilityTranslator,
            Graph graph
    ) {
        this.longRunningProcessRepository = longRunningProcessRepository;
        this.ontologyRepository = ontologyRepository;
        this.privilegeRepository = privilegeRepository;
        this.workspaceHelper = workspaceHelper;
        this.workspaceRepository = workspaceRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.structuredIngestParserFactory = structuredIngestParserFactory;
        this.workQueueRepository = workQueueRepository;
        this.graph = graph;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Required(name = "mapping") String mappingStr,
            @Optional(name = "parseOptions") String parseOptionsStr,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
//        Authorizations structuredFileAuthorizations = graph.createAuthorizations(authorizations, )
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            throw new VisalloResourceNotFoundException("Could not find vertex:" + graphVertexId);
        }

        ElementMutation<Vertex> m = vertex.prepareMutation();
        Visibility visibility = visibilityTranslator.getDefaultVisibility();

        JSONObject mapping = new JSONObject();
        mapping.put("mappedObjects", new JSONObject(mappingStr));
        mapping.put("parseOptions", new JSONObject(parseOptionsStr));


        StructuredIngestOntology.MAPPING.addPropertyValue(m, PROPERTY_KEY, mapping.toString(), visibility);

        m.save(authorizations);
        graph.flush();

        return VisalloResponse.SUCCESS;
    }
}
