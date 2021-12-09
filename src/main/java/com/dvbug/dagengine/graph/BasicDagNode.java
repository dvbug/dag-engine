package com.dvbug.dagengine.graph;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * DAG节点基类
 */
@Slf4j
@Getter
abstract class BasicDagNode implements DagNode {
    private final String name;
    private final DagNodeType type;

    public BasicDagNode(String name, DagNodeType type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public final GraphData execute(@NonNull GraphData input) {
        // 如果返回结果为null, 或在方法内抛出异常, 或 GraphData.isSucceed() = false 都被认为是执行失败
        // 在执行结束后,将输入参数中携带的前置执行历史转移到返回结果中,同时将本次参数压栈到最新历史中
        GraphData result;
        try {
            doExecute(input);
            result = doExecute(input);
            if (null == result) {
                result = GraphData.ofFailure(new NullPointerException(String.format("%s execute result is null", this.getClass().getSimpleName())));
            }
        } catch (Throwable t) {
            result = GraphData.ofFailure(t);
        }

        if (!result.isSucceed()) {
            if (result instanceof GraphData.GraphDataFailureHolder) {
                if (((GraphData.GraphDataFailureHolder) result).getThrowable() instanceof DagAbortException) {
                    log.trace("abort exe {} node with param: {}", name, input);
                } else {
                    log.error(String.format("failed exe %s node, some exception raising. param: %s", name, input), ((GraphData.GraphDataFailureHolder) result).getThrowable());
                }
            } else {
                log.warn("failed exe {} node, set by user. param: {}, result: {}", name, input, result);
            }
        } else {
            log.trace("exe {} node with param: {}, result: {}", name, input, result);
        }
        return result;
    }

    /**
     * 子类核心执行方法
     * 如果返回结果为null, 或在方法内抛出异常, 或 {@link GraphData#isSucceed()} = false 都被认为是执行失败
     * 如果期望下游继续执行:
     * 返回的结果 {@link GraphData#isSucceed()} 必须为true
     * 业务自己保证下游节点认识并能够处理这个 {@link GraphData} 返回对象
     * 如果不期望下游继续执行:
     * 1) 调用 {@link BasicDagNode#abort()} 结束本次执行和下游调用链, 推荐
     * 2) 可以返回 {@link null}, 不推荐, 除非 {@link null} 不属于正确结果
     * 3) 可以显示抛出任意异常, 不推荐, 因为系统无法知道是业务的代码异常导致的执行失败还是业务主动抛出
     *
     * @param input 本次执行需要的参数
     * @return 本次执行结果
     * @throws Throwable 方法执行异常
     */
    protected abstract GraphData doExecute(GraphData input) throws Throwable;

    /**
     * 主动中止本次执行也终止下游依赖业务执行
     * 在开关策略中使用
     */
    protected void abort() {
        throw abortError();
    }

    /**
     * 主动中止本次执行也终止下游依赖业务执行
     * 在开关策略中使用
     */
    protected DagAbortException abortError() {
        return new DagAbortException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicDagNode that = (BasicDagNode) o;
        return name.equals(that.name) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
}
