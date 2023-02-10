package com.fzk.graph.minTree;

import com.fzk.graph.Edge;
import com.fzk.graph.MyGraph;
import com.fzk.graph.Node;

import java.util.HashSet;
import java.util.PriorityQueue;

/**
 * 无向图生成最小生成树
 * prim算法
 * 先随机选一个节点，以它出发每次选择最小的边的新节点，每次将新的节点加入最小树节点集合，下次又以该最小树集合中所有节点出发找新节点最小的边，直接所有节点入树
 *
 * @author fzk
 * @datetime 2022-09-05 17:35
 */
public class PrimMST {
    // 要求图必须是连续的
    // 若图连续，则相当于森林，多个图
    // 在下面取firstNode时，for循环所有节点去生成树即可生成多棵最小生成树
    public static HashSet<Edge> primMst(MyGraph graph) {
        // 当前构建的树包含的节点
        HashSet<Node> mstNodes = new HashSet<>();
        // 从这些节点出发可选的边
        PriorityQueue<Edge> queue = new PriorityQueue<>((a, b) -> a.weight - b.weight);
        // 最小生成树的边
        HashSet<Edge> mstEdges = new HashSet<>();


        Node firstNode = graph.nodes.values().iterator().next();
        mstNodes.add(firstNode);
        addAllEdgeToQueue(mstNodes, queue, firstNode);

        while (mstNodes.size() < graph.nodes.size() && queue.size() > 0) {
            Edge edge = queue.poll();
            // 此边的两个节点都已在树中，忽略
            if (mstNodes.contains(edge.from) && mstNodes.contains(edge.to)) continue;

            // 此最小边可用
            mstEdges.add(edge);// 新最小边入树
            Node next = null;
            // 肯定是to节点为新节点
            if (mstNodes.contains(edge.from)) next = edge.to;
            else next = edge.from;

            mstNodes.add(next);// 新节点入树
            addAllEdgeToQueue(mstNodes, queue, next);// 将当前新节点的新边加入队列中
        }
        return mstEdges;
    }

    // 加入某个新节点的新边入队列
    private static void addAllEdgeToQueue(HashSet<Node> mstNodes, PriorityQueue<Edge> queue, Node node) {
        for (Edge edge : node.edges) {
            // 如果头尾两个节点都包含在树中了，则该边无没必要进队列
            if (!mstNodes.contains(edge.to)) queue.add(edge);
        }
    }
}
