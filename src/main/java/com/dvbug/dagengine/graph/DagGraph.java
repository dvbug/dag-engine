package com.dvbug.dagengine.graph;

import java.util.List;

public interface DagGraph {
    String getGraphId();

    boolean isWhole();

    void addNode(DagNode dagNode);

    void addEdge(DagNode dagNode, DagNode dependOn);

    /**
     * 获取指定节点的下游节点列表
     *
     * @param node 需要查询的节点
     * @return 下游节点列表
     */
    List<DagNode> getNodeChildren(DagNode node);

    List<List<DagNode>> getPaths();

    DagNode getRootS();

    DagNode getFinalS();
}
