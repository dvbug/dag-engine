package com.dvbug.dagengine;

/**
 * DAG 节点
 */
public interface DagNode {
    /**
     * 获取节点名称
     * @return 节点名称, 需要唯一
     */
    String getName();

    /**
     * 获取节点类型
     *
     * @return 节点类型
     */
    DagNodeType getType();

    /**
     * 节点执行方法
     * 如果返回结果为null, 或在方法内抛出异常, 或 {@link GraphData#isSucceed()} = false 都被认为是执行失败
     * 如果期望主动中止,建议抛出 {@link DagAbortException}
     * @param input 输入
     * @return 输出
     */
    GraphData execute(GraphData input);
}
