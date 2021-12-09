package com.dvbug.dagengine.graph;

import lombok.*;

@Getter
@Setter(AccessLevel.PACKAGE)
//@Accessors(chain = true)
@ToString
@RequiredArgsConstructor
public class GraphData {
    private final boolean succeed;
    private final Object result;
    private final Class<?> resultType;

    /**
     * 构建一个指定{@link T}类型的成功结果数据
     * @param input 结果对象
     */
    public static <T> GraphData ofInputParam(T input) {
        return ofSucceed(input);
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