package org.visallo.core.model.longRunningProcess;

import com.google.inject.Injector;
import org.json.JSONObject;
import org.mockito.Mock;
import org.visallo.core.status.MetricsManager;
import org.visallo.core.util.VisalloInMemoryTestBase;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class LongRunningProcessWorkerTestBase extends VisalloInMemoryTestBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(LongRunningProcessWorkerTestBase.class);
    @Mock
    private Injector injector;
    @Mock
    private MetricsManager metricsManager;
    @Mock
    private com.codahale.metrics.Counter mockCounter;
    @Mock
    private com.codahale.metrics.Timer mockTimer;
    @Mock
    private com.codahale.metrics.Meter mockMeter;

    public void before() {
        when(metricsManager.counter(any())).thenReturn(mockCounter);
        when(metricsManager.counter(any(), any())).thenReturn(mockCounter);
        when(metricsManager.timer(any())).thenReturn(mockTimer);
        when(metricsManager.timer(any(), any())).thenReturn(mockTimer);
        when(metricsManager.meter(any())).thenReturn(mockMeter);
        when(metricsManager.meter(any(), any())).thenReturn(mockMeter);
    }

    protected void prepare(LongRunningProcessWorker worker) {
        worker.prepare(getLongRunningWorkerPrepareData());
    }

    protected LongRunningWorkerPrepareData getLongRunningWorkerPrepareData() {
        return new LongRunningWorkerPrepareData(
                getConfig(),
                getUser(),
                getInjector()
        );
    }

    private Injector getInjector() {
        return injector;
    }

    protected Map getConfig() {
        return new HashMap();
    }

    protected MetricsManager getMetricsManager() {
        return metricsManager;
    }

    protected void run(LongRunningProcessWorker worker, JSONObject queueItem) {
        if (worker.isHandled(queueItem)) {
            worker.process(queueItem);
        } else {
            LOGGER.warn("Unhandled: %s", queueItem.toString());
        }
    }
}
