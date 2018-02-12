package org.visallo.vertexium.model.longRunningProcess;

import org.visallo.core.model.longRunningProcess.LongRunningProcessRepository;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRepositoryTestBase;

public class VertexiumLongRunningProcessRepositoryTest extends LongRunningProcessRepositoryTestBase {
    @Override
    public LongRunningProcessRepository getLongRunningProcessRepository() {
        return new VertexiumLongRunningProcessRepository(
                getGraphRepository(),
                getGraphAuthorizationRepository(),
                getUserRepository(),
                getWorkQueueRepository(),
                getGraph(),
                getAuthorizationRepository()
        );
    }
}