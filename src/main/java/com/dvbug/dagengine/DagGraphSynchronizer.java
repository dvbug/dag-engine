package com.dvbug.dagengine;

import io.github.avivcarmis.javared.executor.RedSynchronizer;
import io.github.avivcarmis.javared.future.RedFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * DAG图调度同步器, 线程安全的
 */
@Slf4j
final class DagGraphSynchronizer extends RedSynchronizer<GraphData, GraphData> {
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(24);

    /**
     * 内部子路径同步器
     */
    @RequiredArgsConstructor
    static class SubChainSynchronizer extends RedSynchronizer<GraphData, GraphData> {
        private final java.util.function.Function<GraphData, Result<GraphData>> subChain;

        @Override
        protected Result<GraphData> handle(GraphData graphData) {
            return subChain.apply(graphData);
        }
    }

    private final DagGraph graph;
    // 全路径调用链
    @Deprecated
    private final List<java.util.function.Function<GraphData, Result<GraphData>>> chainFuncList = null;
    // 最优调用链
    private final java.util.function.Function<GraphData, Result<GraphData>> chainFunc;

    public DagGraphSynchronizer(DagGraph graph) {
        this.graph = graph;

        if (!graph.isWhole()) {
            throw new IllegalArgumentException(String.format("Graph[%s] is not whole", graph.getGraphId()));
        }

        chainFunc = autoChainBuild();
    }


    // 从根节点开始构建一条可能成功的调用链
    private java.util.function.Function<GraphData, Result<GraphData>> autoChainBuild() {
        return (input) -> {
            Result<GraphData> chain = produce(GraphData.class).byExecuting(() -> input);
            return buildDeepin(chain, graph.getRootS());
        };
    }

    // 深度递归构建
    private Result<GraphData> buildDeepin(Result<GraphData> chain, DagNode node) {
        Result<GraphData> currentChain = appendChain(chain, node);
        List<DagNode> children = graph.getNodeChildren(node);
        if (children.isEmpty()) { //终止节点
            // fix 在最终结果中把自己加入到History中
            return ifResult(currentChain).succeed().produce(GraphData.class).byExecuting(r-> {
                GraphData newResultWithFinalHistory =
                        r.isSucceed() ?  GraphData.ofSucceed(r.getResult()) :  GraphData.ofFailure((Throwable) r.getResult());
                newResultWithFinalHistory.cloneHistory(r).setNodeName(r.getNodeName());
                r.getHistory().clear();
                newResultWithFinalHistory.pushHistory(r);
                return newResultWithFinalHistory;
            });
        } else if (children.size() == 1) { //单出边情况
            return buildDeepin(currentChain, children.get(0));
        } else { //多出边情况
            //构建下游多子路径触发执行,选者一个有效的执行结果再构建路径返回

            // 构建闭包函数, 在execute运行时将node的输出作为子路径的输入
            java.util.function.Function<GraphData, Result<GraphData>> subF = (input) -> {
                Result<GraphData> subRoot = produce(GraphData.class).byExecuting(() -> input);

                Result[] subChains = children.stream().map(c -> buildDeepin(subRoot, c)).toArray(Result[]::new);

                return ifResults(subChains).finish().produce(GraphData.class) //子路径可能存在失败或全部失败,所以用finish标记
                        .byExecuting(rs -> {
                            // 对于策略引擎来说,正确结果数量 <=1 个
                            // 因此只需要返回第一个成功的结果或者返回null表示所有子路径全部失败
                            GraphData finalResult = null;
                            for (int i = 0; i < subChains.length; i++) {
                                GraphData result = rs.result(i, GraphData.class);
                                if (null == finalResult && null != result && result.isSucceed()) {
                                    finalResult = result;
                                } else if (null != result && !result.isSucceed()) {
                                    //log.trace("popF: {}", result);
                                }
                            }
                            return finalResult;
                        });
            };
            return ifResult(currentChain).succeed()
                    .produceFutureOf(GraphData.class)
                    // 使用SubChainSynchronizer是因为,无法在 byExecuting 阶段内
                    // 将 Result<StrategyData> 对象转化为 StrategyData 或 Future<StrategyData>
                    // Result._future 对象只能在 SubChainSynchronizer 内部获取
                    .byExecuting(new SubChainSynchronizer(subF)::execute);
        }
    }

    //将 node 节点的执行挂载到上游调用链成功的情况下, 上游不成功则什么也不做
    private Result<GraphData> appendChain(Result<GraphData> parent, DagNode node) {
        return ifResult(parent).succeed().produceFutureOf(GraphData.class).byExecuting(r -> {
            if (r.isSucceed()) {
                return futureOf(node.execute(r));
            } else {
                return futureOf(r);
            }
        });
    }

    @Override
    protected Result<GraphData> handle(GraphData input) {
        if (input.isSucceed()) {
            return chainFunc.apply(input);
        } else {
            return produce(GraphData.class).byExecuting(() -> GraphData.ofFailure(new IllegalStateException("first must success input", (Throwable) input.getResult())));
        }
    }

    // 全路径构建
    @Deprecated
    private List<java.util.function.Function<GraphData, Result<GraphData>>> allChainBuild() {
        return graph.getPaths().stream().map(path -> (java.util.function.Function<GraphData, Result<GraphData>>) (input) -> {
            Result<GraphData> chain = produce(GraphData.class).byExecuting(() -> input);
            for (DagNode s : path) {
                chain = appendChain(chain, s);
            }
            return chain;
        }).collect(Collectors.toList());
    }


    // 不使用多路径并行方式, 父级公共节点会被相同入参多次执行
    @Deprecated
    private Result<GraphData> processMulti(GraphData input) {
        Result[] results = chainFuncList.stream().map(f -> f.apply(input)).toArray(Result[]::new);
        return ifResults(results).finish().produce(GraphData.class)
                .byExecuting(rs -> {
                    GraphData finalResult = null;
                    for (int i = 0; i < results.length; i++) {
                        GraphData result = rs.result(i, GraphData.class);
                        //log.info("result: {}", result);
                        if (null == finalResult && result.isSucceed()) {
                            finalResult = result;
                        }
                    }
                    return finalResult;
                });
    }

    private static <T> Future<T> futureOf(T value) {
        CompletableFuture<T> future = new CompletableFuture<>();
        scheduleTask(0, () -> future.complete(value));
        return future;
    }

    public static void scheduleTask(long delayMillis, Runnable runnable) {
        scheduleTask(delayMillis, TimeUnit.MILLISECONDS, runnable);
    }

    public static void scheduleTask(long delay, TimeUnit unit, Runnable runnable) {
        SCHEDULER.schedule(runnable, delay, unit);
    }
}
