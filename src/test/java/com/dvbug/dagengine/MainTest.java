package com.dvbug.dagengine;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.avivcarmis.javared.future.RedFutureOf;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class MainTest {

    private static final ExecutorService POOL = Executors.newFixedThreadPool(24);
    private static final Gson GsonUtil = new GsonBuilder()
            .registerTypeAdapter(new TypeToken<Class<?>>() {
            }.getType(), new GenericClassTypeAdapter())
            .create();


    final static DagGraph graph = new DagGraph("graph123");

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
            } else {
                return GraphData.ofFailure(((Throwable) input.getResult()));
            }
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

        log.info(graph.getAdjacentMatrix().print());
    }

    private void exe(DagGraphSynchronizer synchronizer, GraphData param) {
        RedFutureOf<GraphData> haha = synchronizer.execute(param);
        try {
            GraphData result = haha.get(1000, TimeUnit.MILLISECONDS);
            //log.info("finalResult: {}, history: \n{}", result, Arrays.stream(synchronizer.getHistory()).map(StrategyData::toString).collect(Collectors.joining("\n")));
            log.info("finalResult: {}", result);
            if (null != result) {
                System.out.println(result.getHistory().stream().map(GsonUtil::toJson).collect(Collectors.joining("\n")));
            }
            if (null != result && !result.isSucceed()) {
                log.error("err:", (Throwable) result.getResult());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test() {
        DagGraphSynchronizer synchronizer = new DagGraphSynchronizer(graph);
        GraphData param = GraphData.ofInputParam("Haha");
        exe(synchronizer, param);
    }


    @Test
    public void test2() {
        DagGraphSynchronizer synchronizer = new DagGraphSynchronizer(graph);

        for (int i = 0; i < 10000; i++) {
            GraphData param = GraphData.ofInputParam("Haha" + i);
            exe(synchronizer, param);
        }
    }

    @Test
    public void test3() {
        DagGraphSynchronizer synchronizer = new DagGraphSynchronizer(graph);

        for (int i = 0; i < 5000; i++) {
            final int N = i;
            POOL.submit(() -> {
                GraphData param = GraphData.ofInputParam("Haha" + N);
                exe(synchronizer, param);
            });
        }

        POOL.shutdown();
        try {
            POOL.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            log.error("Test thread pool await error", e);
        }
    }
}
