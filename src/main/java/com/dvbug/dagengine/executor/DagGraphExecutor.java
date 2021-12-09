package com.dvbug.dagengine.executor;


import com.dvbug.dagengine.graph.DagGraph;
import com.dvbug.dagengine.graph.GraphData;
import com.dvbug.javared.future.RedFutureOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * DAG图调度同步器包装类, 线程安全的
 */
public final class DagGraphExecutor {
    private final StrategyGraphSynchronizer synchronizer;

    public DagGraphExecutor(DagGraph graph) {
        this.synchronizer = new StrategyGraphSynchronizer(graph);
    }

    public ExecuteResult<GraphData> execute(GraphData input, long timeout) throws ExecutionException, InterruptedException, TimeoutException {
        RedFutureOf<ExecuteContext> future = this.synchronizer.execute(new ExecuteContext(input, "input"));
        ExecuteContext executeContext = future.get(timeout, TimeUnit.MILLISECONDS);

        GraphData data = null;
        List<ExecuteResult.History<GraphData>> history = null;
        if(null != executeContext) {
            data = executeContext.getData();
            history = Arrays.stream(executeContext.getHistory()).map(h -> new ExecuteResult.History<>(h.getResult(), h.getExecuteNode())).collect(Collectors.toList());
        }

        if(null == data) {
            data = GraphData.ofFailure(new NullPointerException("no result"));
        }
        if(null == history) {
            history = new ArrayList<>();
        }
        return new ExecuteResult<>(data, history);
    }
}
