package com.dvbug.dagengine;

import lombok.Getter;

/**
 * DAG图终节点
 */
@Getter
public final class FinalDagNode extends BasicDagNode {
    public FinalDagNode() {
        super("final", DagNodeType.FINAL);
    }

    // 什么也不做,透传
    @Override
    public GraphData doExecute(GraphData input) {
        return input;
    }
}