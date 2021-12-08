package com.dvbug.dagengine;

import lombok.Getter;

/**
 * DAG图根节点,起始节点
 */
@Getter
public final class RootDagNode extends BasicDagNode {
    public RootDagNode() {
        super("root", DagNodeType.ROOT);
    }

    // 什么也不做,透传
    @Override
    public GraphData doExecute(GraphData input) {
        return input;
    }
}
