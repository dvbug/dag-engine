package com.dvbug.dagengine;

import io.github.avivcarmis.javared.future.RedFutureOf;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * DAG图调度同步器包装类, 线程安全的
 */
public final class DagGraphExecutor {
    private final DagGraphSynchronizer synchronizer;

    public DagGraphExecutor(DagGraph graph) {
        this.synchronizer = new DagGraphSynchronizer(graph);
    }

    public GraphData execute(GraphData input, long timeout) throws ExecutionException, InterruptedException, TimeoutException {
        RedFutureOf<GraphData> future = this.synchronizer.execute(input);
        return future.get(timeout, TimeUnit.MILLISECONDS);
    }
}
