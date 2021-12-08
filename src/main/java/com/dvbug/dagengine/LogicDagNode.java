package com.dvbug.dagengine;

import lombok.Getter;

/**
 * DAG图逻辑节点,业务节点基类
 */
@Getter
public abstract class LogicDagNode extends BasicDagNode {
    public LogicDagNode(String name) {
        super(name, DagNodeType.LOGIC);
    }
}
