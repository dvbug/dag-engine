package com.dvbug.dagengine.graph;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * 图I/O执行数据接口
 */
public interface GraphData {
    boolean isSucceed();

    @Getter
    @ToString
    @RequiredArgsConstructor
    class GraphDataFailureHolder implements GraphData {
        private final boolean succeed = false;
        private final Throwable throwable;
    }

    static GraphDataFailureHolder ofFailure(Throwable throwable) {
        return new GraphDataFailureHolder(throwable);
    }
}