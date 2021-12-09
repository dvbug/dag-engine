package com.dvbug.dagengine;

import com.dvbug.dagengine.executor.DagGraphExecutor;
import com.dvbug.dagengine.executor.ExecuteResult;
import com.dvbug.dagengine.graph.*;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class MainTest {

    static final ExecutorService POOL = Executors.newFixedThreadPool(24);

    final static StrategyGraph graph = new StrategyGraph("graph123");
    static String RESULT_CHECK_VAL;

    static void assertPass(GraphData result) {
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getResult());
        Assertions.assertTrue((result.getResult() instanceof String) && ((String) result.getResult()).contains(RESULT_CHECK_VAL),
                String.format("actual result '%s' not contains key result '%s'", result.getResult(), RESULT_CHECK_VAL));
    }

    @Getter
    @Setter
    public static abstract class DebugLogicDagNode extends LogicDagNode {
        private boolean isThrowable;

        public DebugLogicDagNode(String name) {
            super(name);
        }

        @Override
        protected GraphData doExecute(GraphData input) throws Throwable {
            if (isThrowable()) {
                abort();
            }
            return null;
        }
    }

    public static class StringDagNode extends DebugLogicDagNode {

        public StringDagNode(String name) {
            super(name);
        }

        @Override
        public GraphData doExecute(GraphData input) throws Throwable {
            super.doExecute(input);
            if (input.isSucceed()) {
                return GraphData.ofSucceed(String.format("%s+%s", input.getResult(), getName()));
            }
            throw abortError();
        }
    }

    @BeforeAll
    public static void init() {
        RootDagNode rootS = new RootDagNode();
        FinalDagNode finalS = new FinalDagNode();

        StringDagNode s1 = new StringDagNode("s1");
        StringDagNode s2 = new StringDagNode("s2");
        StringDagNode s3 = new StringDagNode("s3");
        StringDagNode s4 = new StringDagNode("s4");
        StringDagNode s5 = new StringDagNode("s5");
        StringDagNode s6 = new StringDagNode("s6");

        StringDagNode i1 = new StringDagNode("i1");
        StringDagNode i2 = new StringDagNode("i2");

        s1.setThrowable(true);
        i2.setThrowable(true);
        RESULT_CHECK_VAL = "+i1+s4+s5+s6";

        //s4.setThrowable(true);
        //s5.setThrowable(true);
        //s6.setThrowable(true);

        //region add nodes & edges to graph
        graph.addNode(rootS);
        graph.addNode(finalS);
        graph.addNode(s1);
        graph.addNode(s2);
        graph.addNode(s3);
        graph.addNode(s4);
        graph.addNode(s5);
        graph.addNode(s6);
        graph.addNode(i1);
        graph.addNode(i2);


        graph.addEdge(s1, rootS);
        graph.addEdge(i1, rootS);
        graph.addEdge(s2, s1);
        graph.addEdge(s3, s1);
        graph.addEdge(s4, i1);
        graph.addEdge(i2, i1);
        graph.addEdge(s5, s3);
        graph.addEdge(s5, s4);
        graph.addEdge(s6, s2);
        graph.addEdge(s6, s5);
        graph.addEdge(s6, i2);
        graph.addEdge(finalS, s6);
        //endregion

        System.out.println(graph.getAdjacentMatrix().print());
    }

    @SneakyThrows
    private void exe(DagGraphExecutor executor, GraphData param) {
        ExecuteResult<GraphData> result = executor.execute(param, 100);
        assertPass(result.getData());
        System.out.printf("result:%n%s%n", result.getData());
        if (!result.getHistory().isEmpty()) {
            System.out.printf("history:%n%s%n", result.getHistory().stream().map(ExecuteResult.History::toString).collect(Collectors.joining("\n")));
        }
        if (!result.getData().isSucceed()) {
            ((Throwable) result.getData().getResult()).printStackTrace();
        }
    }

    @Test
    public void test() {
        DagGraphExecutor executor = new DagGraphExecutor(graph);
        GraphData param = GraphData.ofInputParam("Haha");
        exe(executor, param);
    }


    @Test
    public void test2() {
        DagGraphExecutor executor = new DagGraphExecutor(graph);

        for (int i = 0; i < 10000; i++) {
            GraphData param = GraphData.ofInputParam("Haha" + i);
            exe(executor, param);
        }
    }

    @Test
    public void test3() {
        DagGraphExecutor executor = new DagGraphExecutor(graph);

        for (int i = 0; i < 10000; i++) {
            final int N = i;
            POOL.submit(() -> {
                GraphData param = GraphData.ofInputParam("Haha" + N);
                exe(executor, param);
            });
        }

        POOL.shutdown();
        try {
            POOL.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            System.err.printf("Test thread pool await error: %n");
            e.printStackTrace();
        }
    }
}
