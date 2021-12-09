package com.dvbug.dagengine.graph;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * DAG 图
 */
@Getter
@RequiredArgsConstructor
public class StrategyGraph implements DagGraph {
    private final String graphId;
    private int edgeCount = 0;
    private DagNode rootS;
    private DagNode finalS;
    @Getter(AccessLevel.NONE)
    private final Set<DagNode> nodes = new HashSet<>();
    @Getter(AccessLevel.NONE)
    private final Map<DagNode, Set<DagNode>> children = new HashMap<>();
    private List<List<DagNode>> paths;

    /**
     * 添加节点到DAG图
     *
     * @param dagNode 节点信息
     */
    public void addNode(DagNode dagNode) {
        if (nodes.contains(dagNode)) {
            throw new IllegalArgumentException(String.format("%s is existed in graph[%s]", dagNode, graphId));
        }

        if (dagNode.getType() == DagNodeType.ROOT && nodes.stream().anyMatch(s -> s.getType() == DagNodeType.ROOT)) {
            throw new IllegalArgumentException(String.format("Just set root once in graph[%s]", graphId));
        }

        if (dagNode.getType() == DagNodeType.FINAL && nodes.stream().anyMatch(s -> s.getType() == DagNodeType.FINAL)) {
            throw new IllegalArgumentException(String.format("Just set final once in graph[%s]", graphId));
        }

        if (dagNode.getType() == DagNodeType.ROOT) {
            this.rootS = dagNode;
        }
        if (dagNode.getType() == DagNodeType.FINAL) {
            this.finalS = dagNode;
        }

        nodes.add(dagNode);
    }

    /**
     * 根据名字添加DAG边
     *
     * @param nodeName         边终节点名称
     * @param nodeNameDependOn 边起节点名称
     */
    public void addEdge(String nodeName, String nodeNameDependOn) {
        Optional<DagNode> node = nodes.stream().filter(n -> n.getName().equals(nodeName)).findFirst();
        Optional<DagNode> dependNode = nodes.stream().filter(n -> n.getName().equals(nodeNameDependOn)).findFirst();

        if (!node.isPresent()) {
            throw new IllegalArgumentException(String.format("No %s named [%s] in graph[%s]", DagNode.class.getSimpleName(), nodeName, graphId));
        }
        if (!dependNode.isPresent()) {
            throw new IllegalArgumentException(String.format("No %s named [%s] in graph[%s]", DagNode.class.getSimpleName(), nodeNameDependOn, graphId));
        }

        addEdge(node.get(), dependNode.get());
    }

    /**
     * 添加DAG边
     *
     * @param dagNode  边终节点
     * @param dependOn 边起节点
     */
    public void addEdge(DagNode dagNode, DagNode dependOn) {
        String edgeName = edgeName(dependOn, dagNode);
        if (!nodes.contains(dagNode) || !nodes.contains(dependOn)) {
            throw new IllegalStateException(String.format("Graph edge[%s] can not independent in graph[%s]", edgeName, graphId));
        }

        Set<DagNode> children = this.children.computeIfAbsent(dependOn, k -> new HashSet<>());
        if (children.contains(dagNode)) {
            throw new IllegalStateException(String.format("Graph edge[%s] is existed in graph[%s]", edgeName, graphId));
        }
        children.add(dagNode);
        edgeCount++;
    }


    @Override
    public boolean isWhole() {
        return null != rootS && null != finalS && nodes.size() >= 2;
    }

    /**
     * 获取DAG图的全部可达路径
     */
    public List<List<DagNode>> getPaths() {
        if (null != paths) return paths;

        paths = new ArrayList<>();
        new PathGenerator(this);
        return paths;
    }

    /**
     * 获取指定节点的下游节点列表
     *
     * @param node 需要查询的节点
     * @return 下游节点列表
     */
    @Override
    public List<DagNode> getNodeChildren(DagNode node) {
        return new ArrayList<>(this.children.getOrDefault(node, new HashSet<>()));
    }

    /**
     * 获取DAG邻接矩阵
     */
    public AdjacentMatrix getAdjacentMatrix() {
        return new AdjacentMatrix(this);
    }

    /**
     * 内部类,DAG图全路径构造器
     */
    class PathGenerator {
        Stack<DagNode> nodes = new Stack<>();

        public PathGenerator(StrategyGraph graph) {
            find(getRootS(), graph);
        }

        private void find(DagNode node, StrategyGraph graph) {
            nodes.push(node);
            Set<DagNode> children = graph.children.getOrDefault(node, new HashSet<>());
            if (!children.isEmpty()) {
                for (final DagNode child : children) {
                    find(child, graph);
                }
                nodes.pop();
            } else {
                paths.add(new ArrayList<>(nodes));
                if (!nodes.isEmpty()) {
                    nodes.pop();
                }
            }
        }
    }

    /**
     * 内部类 DAG邻接矩阵
     */
    @Getter
    public class AdjacentMatrix {
        private final String graphId;
        private final DagNode[] nodes;
        private final int[][] matrix;

        AdjacentMatrix(StrategyGraph graph) {
            this.graphId = graph.getGraphId();
            Pair<DagNode[], int[][]> pair = buildAdjacencyMatrix(graph);
            this.nodes = pair.getLeft();
            this.matrix = pair.getRight();
        }

        public String print() {
            StringBuilder builder = new StringBuilder();
            builder.append("DAG GRAPH ADJACENCY MATRIX INFOS").append("\n");
            builder.append("graphId: ").append(graphId).append("\n");
            builder.append("nodes:\n");
            for (int i = 0; i < nodes.length; i++) {
                builder.append("(").append(i).append(")").append(nodes[i].getName());
                if (i < nodes.length - 1) builder.append(", ");
            }
            builder.append("\n");
            builder.append("--------------------------------\n");
            builder.append("matrix:\n");
            for (int i = 0; i < matrix.length; i++) {
                builder.append("(").append(i).append("): ").append(Arrays.toString(matrix[i])).append("\n");
            }
            return builder.toString();
        }

        private Pair<DagNode[], int[][]> buildAdjacencyMatrix(StrategyGraph graph) {
            DagNode[] nodes = graph.nodes.toArray(new DagNode[0]);
            int size = nodes.length;
            int[][] matrix = new int[size][size];
            for (int i = 0; i < size; i++) {
                DagNode node = nodes[i];
                for (int j = 0; j < nodes.length; j++) {
                    boolean contains = children.getOrDefault(node, new HashSet<>()).contains(nodes[j]);
                    matrix[i][j] = contains ? 1 : 0;
                }
            }
            return new ImmutablePair<>(nodes, matrix);
        }
    }

    private String edgeName(DagNode from, DagNode to) {
        return String.format("%s->%s", from.getName(), to.getName());
    }
}
