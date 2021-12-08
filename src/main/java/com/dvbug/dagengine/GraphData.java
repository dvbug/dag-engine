package com.dvbug.dagengine;

import lombok.*;

import java.util.Stack;

@Getter
@Setter(AccessLevel.PACKAGE)
//@Accessors(chain = true)
@ToString(exclude = {"history"})
@RequiredArgsConstructor
public class GraphData {
    private final boolean succeed;
    private final Object result;
    private final Class<?> resultType;
    @Getter(AccessLevel.MODULE)
    private String nodeName;
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.PACKAGE)
    private transient final Stack<GraphData> history = new Stack<>();

    GraphData setNodeName(String nodeName) {
        this.nodeName = nodeName;
        return this;
    }

    /**
     * 转移 {@link data} 携带的历史到本实例中
     */
    GraphData cloneHistory(GraphData data) {
        if (null != data && !data.getHistory().isEmpty()) {
            this.history.clear();
            data.getHistory().forEach(this.history::push);
        }
        return this;
    }

    /**
     * 清空 {@link data} 携带的历史,同时将 {@link data} 压栈为最新历史
     */
    GraphData pushHistory(GraphData data) {
        if (null != data) {
            data.getHistory().clear();
            this.history.push(data);
        }

        return this;
    }

    /**
     * 构建一个指定{@link T}类型的成功结果数据
     * @param input 结果对象
     */
    public static <T> GraphData ofInputParam(T input) {
        return ofSucceed(input).setNodeName("input");
    }

    /**
     * 构建一个指定{@link T}类型的成功结果数据
     * @param result 结果数据对象
     */
    public static <T> GraphData ofSucceed(T result) {
        return new GraphData(true, result, result.getClass());
    }

    /**
     * 构建一个指定{@link Throwable}异常的失败结果数据
     * @param throwable 需要包装的异常实例
     */
    public static GraphData ofFailure(Throwable throwable) {
        return new GraphData(false, throwable, throwable.getClass());
    }

    /**
     * 构建一个包括 {@link DagAbortException} 的失败结果数据
     */
    public static GraphData ofAbort() {
        return ofFailure(new DagAbortException());
    }
}