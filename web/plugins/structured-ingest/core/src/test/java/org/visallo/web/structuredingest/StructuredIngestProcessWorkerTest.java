package org.visallo.web.structuredingest;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.semanticweb.owlapi.model.IRI;
import org.vertexium.*;
import org.vertexium.inmemory.InMemoryAuthorizations;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.util.StreamUtils;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.longRunningProcess.LongRunningProcessWorkerTestBase;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyPropertyDefinition;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserPropertyPrivilegeRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.model.PropertyType;
import org.visallo.web.clientapi.model.SandboxStatus;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.visallo.web.structuredingest.core.model.ClientApiAnalysis;
import org.visallo.web.structuredingest.core.model.ParseOptions;
import org.visallo.web.structuredingest.core.model.StructuredIngestParser;
import org.visallo.web.structuredingest.core.model.StructuredIngestQueueItem;
import org.visallo.web.structuredingest.core.util.BaseStructuredFileParserHandler;
import org.visallo.web.structuredingest.core.util.StructuredIngestParserFactory;
import org.visallo.web.structuredingest.core.util.mapping.ParseMapping;
import org.visallo.web.structuredingest.core.util.mapping.PropertyMapping;
import org.visallo.web.structuredingest.core.worker.StructuredIngestProcessWorker;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.visallo.web.structuredingest.mapping.MappingTestHelpers.createIndexedMap;

@RunWith(MockitoJUnitRunner.class)
public class StructuredIngestProcessWorkerTest extends LongRunningProcessWorkerTestBase {
    private static final String WORKSPACE_ID = "testWorkspaceId";
    private static final String PARSE_MAPPING_FILE_NAME = "parsemapping.json";
    private static final String OWL_BASE_URI = "http://visallo.org/structured-file-test/";
    private static final String PARSE_MAPPING_DYNAMIC_ONTOLOGY_FILE_NAME = "parsemappingdynamicontology.json";
    private static final String DYNAMIC_ONTOLOGY_PROPERTY_IRI = OWL_BASE_URI + "testproperty#testWorkspaceId";
    private static final String DYNAMIC_ONTOLOGY_TEST_CONCEPT1_IRI = OWL_BASE_URI + "testconcept1#testWorkspaceId";
    private static final String DYNAMIC_ONTOLOGY_TEST_CONCEPT2_IRI = OWL_BASE_URI + "testconcept2#testWorkspaceId";
    private static final String DYNAMIC_ONTOLOGY_RELATIONSHIP_IRI = OWL_BASE_URI + "testrelationship#testWorkspaceId";
    private Authorizations authorizations = new InMemoryAuthorizations(WORKSPACE_ID);


    private StructuredIngestProcessWorker structuredIngestProcessWorker;
    private Vertex structuredFileVertex;
    private User user;

    @Before
    public void before() {
        super.before();
        structuredIngestProcessWorker = new StructuredIngestProcessWorker();
        structuredIngestProcessWorker.setMetricsManager(getMetricsManager());
        prepare(structuredIngestProcessWorker);

        getOntologyRepository().importResourceOwl(this.getClass(), "sample.owl", "http://visallo.org/structured-file-test", authorizations);

        VertexBuilder structuredFileVertexBuilder = getGraph().prepareVertex(new Visibility(""));
        VisibilityJson visibilityJson = VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(new VisibilityJson(), "", WORKSPACE_ID);
        VisalloProperties.VISIBILITY_JSON.setProperty(structuredFileVertexBuilder, visibilityJson, new Visibility(""));
        VisalloProperties.MIME_TYPE.addPropertyValue(structuredFileVertexBuilder, "", "text/csv", new Visibility(""));
        try {
            InputStream inputStream = IOUtils.toInputStream("John Smith,3/13/2015,yes", "UTF-8");
            VisalloProperties.RAW.setProperty(structuredFileVertexBuilder, new StreamingPropertyValue(inputStream, String.class), new Visibility(""));
        } catch (IOException e) {
            throw new VisalloException("Could not convert row data to inputstream", e);
        }
        structuredFileVertex = structuredFileVertexBuilder.save(authorizations);

        user = getUserRepository().findOrAddUser(
                "junit",
                "JUnit",
                "junit@v5analytics.com",
                "password"
        );
        getWorkspaceRepository().add(WORKSPACE_ID, "Default Junit", user);

        structuredIngestProcessWorker.setOntologyRepository(getOntologyRepository());
        structuredIngestProcessWorker.setGraph(getGraph());
        structuredIngestProcessWorker.setVisibilityTranslator(getVisibilityTranslator());
        structuredIngestProcessWorker.setUserRepository(getUserRepository());
        structuredIngestProcessWorker.setWorkspaceRepository(getWorkspaceRepository());
        structuredIngestProcessWorker.setPrivilegeRepository(getPrivilegeRepository());
        structuredIngestProcessWorker.setLongRunningProcessRepository(getLongRunningProcessRepository());
        StructuredIngestParserFactory structuredIngestParserFactory = new StructuredIngestParserFactory(getConfiguration()) {
            @Override
            public StructuredIngestParser getParser(String mimeType) {
                return new TestParser();
            }
        };
        structuredIngestProcessWorker.setStructuredIngestParserFactory(structuredIngestParserFactory);
    }

    @Test
    public void testIngestPublishImmediatelyWithNoDynamicOntologyFails() throws Exception {
        JSONObject structuredIngestQueueItem = setupWorker(true, PARSE_MAPPING_FILE_NAME);
        structuredIngestProcessWorker.process(structuredIngestQueueItem);
        assertResult(SandboxStatus.PRIVATE, 3);
    }


    @Test
    public void testIngestPublishImmediatelyWithNoDynamicOntologySucceeds() throws Exception {
        setupUserPrivileges(Sets.newHashSet(Privilege.PUBLISH));
        JSONObject structuredIngestQueueItem = setupWorker(true, PARSE_MAPPING_FILE_NAME);
        structuredIngestProcessWorker.process(structuredIngestQueueItem);
        assertResult(SandboxStatus.PUBLIC, 3);
    }

    @Test
    public void testIngestNotPublishingImmediatelySucceeds() throws Exception {
        JSONObject structuredIngestQueueItem = setupWorker(false, PARSE_MAPPING_FILE_NAME);
        structuredIngestProcessWorker.process(structuredIngestQueueItem);
        assertResult(SandboxStatus.PRIVATE, 3);
    }

    @Test
    public void testIngestPublishImmediatelyWithDynamicOntologyWithoutPrivilegeFails() throws Exception {
        setupUserPrivileges(Sets.newHashSet(Privilege.ONTOLOGY_ADD));
        setupDynamicOntology();
        JSONObject structuredIngestQueueItem = setupWorker(true, PARSE_MAPPING_DYNAMIC_ONTOLOGY_FILE_NAME);
        structuredIngestProcessWorker.process(structuredIngestQueueItem);
        assertResult(SandboxStatus.PRIVATE, 4);
    }

    @Test
    public void testIngestPublishImmediatelyWithDynamicOntologyWithPrivilegeSucceeds() throws Exception {
        setupUserPrivileges(Sets.newHashSet(Privilege.PUBLISH, Privilege.ONTOLOGY_ADD, Privilege.ONTOLOGY_PUBLISH));
        setupDynamicOntology();
        JSONObject structuredIngestQueueItem = setupWorker(true, PARSE_MAPPING_DYNAMIC_ONTOLOGY_FILE_NAME);
        structuredIngestProcessWorker.process(structuredIngestQueueItem);
        assertResult(SandboxStatus.PUBLIC, 4);

        assertNotNull(getOntologyRepository().getPropertyByIRI(DYNAMIC_ONTOLOGY_PROPERTY_IRI, OntologyRepository.PUBLIC));
        assertNotNull(getOntologyRepository().getConceptByIRI(DYNAMIC_ONTOLOGY_TEST_CONCEPT1_IRI, OntologyRepository.PUBLIC));
        assertNotNull(getOntologyRepository().getConceptByIRI(DYNAMIC_ONTOLOGY_TEST_CONCEPT2_IRI, OntologyRepository.PUBLIC));
        assertNotNull(getOntologyRepository().getRelationshipByIRI(DYNAMIC_ONTOLOGY_RELATIONSHIP_IRI, OntologyRepository.PUBLIC));
    }

    @Test
    public void testIngestPublishImmediatelyWithOntologyErrorsDoesNotPublishData() throws Exception {
        setupUserPrivileges(Sets.newHashSet(Privilege.PUBLISH, Privilege.ONTOLOGY_ADD, Privilege.ONTOLOGY_PUBLISH));
        setupDynamicOntology();
        JSONObject structuredIngestQueueItem = setupWorker(true, PARSE_MAPPING_DYNAMIC_ONTOLOGY_FILE_NAME);
        setupUserPrivileges(Sets.newHashSet(Privilege.PUBLISH));
        try {
            structuredIngestProcessWorker.process(structuredIngestQueueItem);
        } catch (VisalloException ex) {
            assertResult(SandboxStatus.PRIVATE, 1);
            return;
        }
        assertTrue(false);
    }

    @Test
    public void testIngestPublishImmediatelyWithErrorsDoesNotPublishSource() throws Exception {
        setupUserPrivileges(Sets.newHashSet(Privilege.PUBLISH, Privilege.ONTOLOGY_ADD, Privilege.ONTOLOGY_PUBLISH));
        setupDynamicOntology();

        InputStream inputStream = IOUtils.toInputStream("John Smith,3/13/2015,you bet", "UTF-8");
        VisalloProperties.RAW.setProperty(structuredFileVertex, new StreamingPropertyValue(inputStream, String.class), new Visibility(""), authorizations);
        getGraph().flush();

        JSONObject structuredIngestQueueItem = setupWorker(true, PARSE_MAPPING_DYNAMIC_ONTOLOGY_FILE_NAME);
        try {
            structuredIngestProcessWorker.process(structuredIngestQueueItem);
        } catch (VisalloException e) {
            assertResult(SandboxStatus.PRIVATE, 1);
            assertNotNull(getOntologyRepository().getPropertyByIRI(DYNAMIC_ONTOLOGY_PROPERTY_IRI, OntologyRepository.PUBLIC));
            assertNotNull(getOntologyRepository().getConceptByIRI(DYNAMIC_ONTOLOGY_TEST_CONCEPT1_IRI, OntologyRepository.PUBLIC));
            assertNotNull(getOntologyRepository().getConceptByIRI(DYNAMIC_ONTOLOGY_TEST_CONCEPT2_IRI, OntologyRepository.PUBLIC));
            assertNotNull(getOntologyRepository().getRelationshipByIRI(DYNAMIC_ONTOLOGY_RELATIONSHIP_IRI, OntologyRepository.PUBLIC));
            return;
        }
        assertTrue(false);
    }

    private void setupUserPrivileges(Set<String> privileges) {
        ((UserPropertyPrivilegeRepository) getPrivilegeRepository()).setPrivileges(user, privileges, getUserRepository().getSystemUser());
    }

    private JSONObject setupWorker(boolean publish, String resourceName) throws IOException {
        InputStream parseMappingJson = this.getClass().getResourceAsStream(resourceName);
        JSONObject structuredIngestQueueItem = new StructuredIngestQueueItem(WORKSPACE_ID,
                IOUtils.toString(parseMappingJson, "UTF-8"),
                user.getUserId(),
                structuredFileVertex.getId(),
                StructuredIngestProcessWorker.TYPE,
                new ParseOptions(),
                publish,
                authorizations).toJson();

        String lrpId = getLongRunningProcessRepository().enqueue(structuredIngestQueueItem, user, authorizations);
        structuredIngestQueueItem.put("id", lrpId);
        return structuredIngestQueueItem;
    }

    private void setupDynamicOntology() {
        Concept testConcept1 = getOntologyRepository().getOrCreateConcept(null, DYNAMIC_ONTOLOGY_TEST_CONCEPT1_IRI, "Test Concept", null, user, WORKSPACE_ID);
        Concept testConcept2 = getOntologyRepository().getOrCreateConcept(null, DYNAMIC_ONTOLOGY_TEST_CONCEPT2_IRI, "Test Concept", null, user, WORKSPACE_ID);
        OntologyPropertyDefinition ontologyPropertyDefinition = new OntologyPropertyDefinition(Arrays.asList(testConcept1), DYNAMIC_ONTOLOGY_PROPERTY_IRI, "Test Property", PropertyType.STRING);
        ontologyPropertyDefinition.setTextIndexHints(TextIndexHint.NONE);
        getOntologyRepository().getOrCreateProperty(ontologyPropertyDefinition, user, WORKSPACE_ID);
        getOntologyRepository().getOrCreateRelationshipType(null, Arrays.asList(testConcept1), Arrays.asList(testConcept2), DYNAMIC_ONTOLOGY_RELATIONSHIP_IRI, true, user, WORKSPACE_ID);

        assertNull(getOntologyRepository().getPropertyByIRI(DYNAMIC_ONTOLOGY_PROPERTY_IRI, OntologyRepository.PUBLIC));
        assertNull(getOntologyRepository().getConceptByIRI(DYNAMIC_ONTOLOGY_TEST_CONCEPT1_IRI, OntologyRepository.PUBLIC));
        assertNull(getOntologyRepository().getConceptByIRI(DYNAMIC_ONTOLOGY_TEST_CONCEPT2_IRI, OntologyRepository.PUBLIC));
        assertNull(getOntologyRepository().getRelationshipByIRI(DYNAMIC_ONTOLOGY_RELATIONSHIP_IRI, OntologyRepository.PUBLIC));
    }

    private void assertResult(SandboxStatus sandboxStatus, int numberOfVertices) {
        Iterable<Vertex> vertices = getGraph().getVertices(authorizations);
        assertEquals("Expected new vertices to be created", numberOfVertices, Iterables.size(vertices));

        StreamUtils.stream(vertices).forEach(vertex -> {
            assertEquals(sandboxStatus, SandboxStatusUtil.getSandboxStatus(vertex, WORKSPACE_ID));
        });
    }

    private PropertyMapping findPropertyMapping(String name, ParseMapping parseMapping) {
        for (int i = 0; i < parseMapping.vertexMappings.size(); i++) {
            for (int j = 0; j < parseMapping.vertexMappings.get(i).propertyMappings.size(); j++) {
                PropertyMapping propertyMapping = parseMapping.vertexMappings.get(i).propertyMappings.get(j);
                if (name.equals(propertyMapping.name)) {
                    return propertyMapping;
                }
            }
        }
        fail("Unable to find fraud property mapping: " + name);
        return null;
    }

    private class TestParser implements StructuredIngestParser {
        @Override
        public Set<String> getSupportedMimeTypes() {
            return null;
        }

        @Override
        public void ingest(InputStream inputStream, ParseOptions parseOptions, BaseStructuredFileParserHandler parserHandler) throws Exception {
            parserHandler.newSheet("A");
            String[] rowValues = IOUtils.toString(inputStream, "UTF-8").split(",");
            Map<String, Object> row = createIndexedMap(rowValues);
            parserHandler.addRow(row, 0);
        }

        @Override
        public ClientApiAnalysis analyze(InputStream inputStream) throws Exception {
            return null;
        }
    }
}
