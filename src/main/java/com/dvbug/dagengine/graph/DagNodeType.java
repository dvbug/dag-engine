package com.dvbug.dagengine.graph;

/**
 * DAG图节点类型
 */
public enum DagNodeType {
    /**
     * 根节点,起始节点
     */
    ROOT,
    /**
     * 逻辑节点
     */
    LOGIC,
    /**
     * 终节点,结束节点
     */
    FINAL
}