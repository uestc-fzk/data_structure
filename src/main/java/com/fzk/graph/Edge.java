package com.fzk.graph;

/**
 * @author fzk
 * @datetime 2022-08-19 10:32
 */
public class Edge {
    public int weight;// 权重
    public Node from;// 边出发点
    public Node to;// 边终点

    public Edge(int weight, Node from, Node to) {
        this.weight = weight;
        this.from = from;
        this.to = to;
    }
}
