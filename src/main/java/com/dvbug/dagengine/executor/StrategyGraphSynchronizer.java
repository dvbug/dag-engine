package com.dvbug.dagengine.executor;

import com.dvbug.dagengine.graph.DagGraph;
import com.dvbug.dagengine.graph.DagNode;
import com.dvbug.dagengine.graph.GraphData;
import com.dvbug.javared.executor.RedSynchronizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * DAG图调度同步器, 线程安全的
 */
@Slf4j
final class StrategyGraphSynchronizer extends RedSynchronizer<ExecuteContext, ExecuteContext> {
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(24);

    /**
     * 内部子路径同步器
     */
    @RequiredArgsConstructor
    static class SubChainSynchronizer extends RedSynchronizer<ExecuteContext, ExecuteContext> {
        final java.util.function.Function<ExecuteContext, Result<ExecuteContext>> subChain;

        @Override
        protected Result<ExecuteContext> handle(ExecuteContext ExecuteContext) {
            return subChain.apply(ExecuteContext);
        }
    }

    private final DagGraph graph;
    // 全路径调用链
    @Deprecated
    private final List<java.util.function.Function<ExecuteContext, Result<ExecuteContext>>> chainFuncList = null;
    // 最优调用链
    private final java.util.function.Function<ExecuteContext, Result<ExecuteContext>> chainFunc;

    public StrategyGraphSynchronizer(DagGraph graph) {
        this.graph = graph;

        if (!graph.isWhole()) {
            throw new IllegalArgumentException(String.format("Graph[%s] is not whole", graph.getGraphId()));
        }

        chainFunc = autoChainBuild();
    }


    // 从根节点开始构建一条可能成功的调用链
    private java.util.function.Function<ExecuteContext, Result<ExecuteContext>> autoChainBuild() {
        return (input) -> {
            Result<ExecuteContext> chain = produce(ExecuteContext.class).byExecuting(() -> input);
            return buildDeepin(chain, graph.getRootNode());
        };
    }

    // 深度递归构建
    private Result<ExecuteContext> buildDeepin(Result<ExecuteContext> chain, DagNode node) {
        Result<ExecuteContext> currentChain = appendChain(chain, node);
        List<DagNode> children = graph.getNodeChildren(node);
        if (children.isEmpty()) { //终止节点
            // fix 在最终结果中把自己加入到History中
            return ifResult(currentChain).succeed().produce(ExecuteContext.class)
                    .byExecuting(r ->
                            new ExecuteContext().clone(r).pushHistory(
                                    new ExecuteContext.ExecuteHistoryItem(r.getData(), r.getExecuteNode())));
        } else if (children.size() == 1) { //单出边情况
            return buildDeepin(currentChain, children.get(0));
        } else { //多出边情况
            //构建下游多子路径触发执行,选者一个有效的执行结果再构建路径返回
            //log.trace("build sub synchronizer for {} node", node.getName());
            // 构建闭包函数, 在execute运行时将node的输出作为子路径的输入
            java.util.function.Function<ExecuteContext, Result<ExecuteContext>> subF = (input) -> {
                Result<ExecuteContext> subRoot = produce(ExecuteContext.class).byExecuting(() -> input);

                Result[] subChains = children.stream().map(c -> buildDeepin(subRoot, c)).toArray(Result[]::new);

                return ifResults(subChains).finish().produce(ExecuteContext.class) //子路径可能存在失败或全部失败,所以用finish标记
                        .byExecuting(rs -> tryGetOneFromMultiResults(rs, subChains.length));
            };
            return ifResult(currentChain).succeed()
                    .produceFutureOf(ExecuteContext.class)
                    // 使用SubChainSynchronizer是因为,无法在 byExecuting 阶段内
                    // 将 Result<StrategyData> 对象转化为 StrategyData 或 Future<StrategyData>
                    // Result._future 对象只能在 SubChainSynchronizer 内部获取
                    .byExecuting(new SubChainSynchronizer(subF)::execute);
        }
    }

    //将 node 节点的执行挂载到上游调用链成功的情况下, 上游不成功则什么也不做
    private Result<ExecuteContext> appendChain(Result<ExecuteContext> parent, DagNode node) {
        return ifResult(parent).succeed().produceFutureOf(ExecuteContext.class).byExecuting(context -> {
            String prevExecuteNode = context.getExecuteNode();
            GraphData prevResult = context.getData();

            ExecuteContext newContext = new ExecuteContext().clone(context);

            if (prevResult.isSucceed()) {
                if (!"input".equals(context.getExecuteNode())) {
                    newContext.pushHistory(new ExecuteContext.ExecuteHistoryItem(prevResult, prevExecuteNode));
                }
                return futureOf(newContext.setData(node.execute(prevResult)).setExecuteNode(node.getName()));
            } else {
                return futureOf(newContext);
            }
        });
    }

    @Override
    protected Result<ExecuteContext> handle(ExecuteContext input) {
        return chainFunc.apply(input);
    }

    // 全路径构建
    @Deprecated
    private List<java.util.function.Function<ExecuteContext, Result<ExecuteContext>>> allChainBuild() {
        return graph.getPaths().stream().map(path -> (java.util.function.Function<ExecuteContext, Result<ExecuteContext>>) (input) -> {
            Result<ExecuteContext> chain = produce(ExecuteContext.class).byExecuting(() -> input);
            for (DagNode s : path) {
                chain = appendChain(chain, s);
            }
            return chain;
        }).collect(Collectors.toList());
    }


    // 不使用多路径并行方式, 父级公共节点会被相同入参多次执行
    @Deprecated
    private Result<ExecuteContext> processMulti(ExecuteContext input) {
        Result[] results = chainFuncList.stream().map(f -> f.apply(input)).toArray(Result[]::new);
        return ifResults(results).finish().produce(ExecuteContext.class)
                .byExecuting(rs -> tryGetOneFromMultiResults(rs, results.length));
    }

    // 对于策略引擎来说,正确结果数量 <=1 个
    // 因此只需要返回第一个成功的结果或者返回null表示所有子路径全部失败
    protected ExecuteContext tryGetOneFromMultiResults(Results results, int resultsSize) {
        ExecuteContext finalResult = null;
        for (int i = 0; i < resultsSize; i++) {
            ExecuteContext result = results.result(i, ExecuteContext.class);

            if (null == finalResult && null != result && null != result.getData() && result.getData().isSucceed()) {
                finalResult = result;
            }
        }
        return finalResult;
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
