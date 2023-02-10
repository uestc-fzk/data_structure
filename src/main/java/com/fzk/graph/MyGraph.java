package com.fzk.graph;

import java.util.HashMap;
import java.util.HashSet;

/**
 * 有向图
 *
 * @author fzk
 * @datetime 2022-08-19 10:29
 */
public class MyGraph {
    public HashMap<Integer, Node> nodes;
    public HashSet<Edge> edges;

    // matrix代表所有边， N*3矩阵
    // matrix[i]=[from节点, to节点, 边权重]
    public MyGraph(int[][] matrix) {
        this.nodes = new HashMap<>();
        this.edges = new HashSet<>();

        // 将输入转为图
        for (int i = 0; i < matrix.length; i++) {
            int from = matrix[i][0], to = matrix[i][1], weight = matrix[i][2];
            Node fromNode = this.nodes.get(from), toNode = this.nodes.get(to);
            if (fromNode == null) {
                fromNode = new Node(from);
                this.nodes.put(from, fromNode);
            }
            if (toNode == null) {
                toNode = new Node(to);
                this.nodes.put(to, toNode);
            }

            Edge edge = new Edge(weight, fromNode, toNode);
            this.edges.add(edge);
            fromNode.nextNodes.add(toNode);
            fromNode.edges.add(edge);
            fromNode.out++;
            toNode.in++;
            // 有向图
            // toNode.nextNodes.add(fromNode);
            // toNode.edges.add(edge);
        }
    }
}
