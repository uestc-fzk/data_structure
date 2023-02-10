package com.fzk.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * 这个算法的思想类似动态规划。适用范围：没有权值为负数的边。这是一种路由选择算法。
 * 此算法用于在图中，从节点A到图中所有节点的最短路劲选择。
 *
 * @author fzk
 * @datetime 2022-09-16 18:18
 */
public class Dijkstra {
    /**
     * 返回从head出发能到达的所有节点的最短距离
     */
    public static HashMap<Node, Integer> dijkstra(Node head) {
        // 目前已知的节点到head节点的最短距离
        HashMap<Node, Integer> distanceMap = new HashMap<>();
        distanceMap.put(head, 0);

        // 已经求过距离的节点，之后不会修改其距离
        HashSet<Node> selectedNodes = new HashSet<>();
        Node minNode = null;
        while ((minNode = getMinDistanceAndUnselectedNode(distanceMap, selectedNodes)) != null) {
            selectedNodes.add(minNode);
            int distance = distanceMap.get(minNode);

            // 新节点入集合后，将其能影响到的其它未入集合节点到head节点的距离进行修正
            for (Edge edge : minNode.edges) {
                Node to = edge.to;
                distanceMap.put(to, Math.min(
                        distanceMap.getOrDefault(to, Integer.MAX_VALUE),
                        distance + edge.weight));
            }
        }
        return distanceMap;
    }

    /**
     * 从未被选入集合中的节点里面找到距离head节点最小距离的minNode
     *
     * @apiNote 这里通过遍历方式来选出最小距离节点，有点挫，可以优化为堆，这个堆得自己维护，距离变小则上浮、距离变大则下沉
     */
    private static Node getMinDistanceAndUnselectedNode(HashMap<Node, Integer> distanceMap, HashSet<Node> selectedNodes) {
        int minDistance = Integer.MAX_VALUE;
        Node minNode = null;
        for (Map.Entry<Node, Integer> entry : distanceMap.entrySet()) {
            Node node = entry.getKey();
            int distance = entry.getValue();
            if (!selectedNodes.contains(node) && distance < minDistance) {
                minNode = node;
                minDistance = distance;
            }
        }
        return minNode;
    }
}
