package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * 拓扑排序以及有向环判断
 *
 * @author fzk
 * @datetime 2022-08-19 15:43
 */
public class TopoSort {
    // 判断有向图是否有环
    // 用途：包依赖解析，避免循环依赖
    public static boolean isHasCycle(MyGraph graph) {
        // 当有某个结点时说明在解析，为true表示解析完成，false表示解析进行中
        HashMap<Node, Boolean> parse = new HashMap<>();

        for (Node node : graph.nodes.values()) {
            dfsIsHasCycle(node, parse);
        }
        return isCycleDependency;
    }

    private static boolean isCycleDependency = false;

    private static void dfsIsHasCycle(Node node, HashMap<Node, Boolean> parse) {
        if (isCycleDependency) return;
        // node解析过了
        if (parse.containsKey(node)) {
            // node正在解析说明出现循环依赖了！
            if (!parse.get(node)) {
                isCycleDependency = true;
            }
            return;
        }

        // 开始解析node
        parse.put(node, false);
        // dfs方式解析其依赖
        for (Node next : node.nextNodes) {
            dfsIsHasCycle(next, parse);
        }
        // 解析完成
        parse.put(node, true);
    }

    // 有向图拓扑排序，也能根据这个判断是否有向环
    // 使用场景：任务依赖调度，如A-->B-->C，则任务调度顺序必须为[A, B, C]
    public static ArrayList<Node> topoSort(MyGraph graph) {
        // 0.有环图无拓扑排序
        //if (isHasCycle(graph)) return null;

        LinkedList<Node> zeroInNode = new LinkedList<>();// 入度为0的节点入队列
        HashMap<Node, Integer> nodeInMap = new HashMap<>();// 节点的剩余入度，若不想改变原始结构，则必须以其它结构保存其入度情况
        ArrayList<Node> result = new ArrayList<>();

        // 1.先查找入度为0的节点，它们最先执行
        for (Node node : graph.nodes.values()) {
            if (node.in == 0) zeroInNode.addLast(node);
            nodeInMap.put(node, node.in);
        }

        // 2.拓扑排序开始，将入度为0的节点逻辑上从有向图中去除，依次改变其它节点入度，一轮轮的找入度为0节点
        while (zeroInNode.size() > 0) {
            Node cur = zeroInNode.removeFirst();
            result.add(cur);
            // 逻辑上移除cur节点，修改其后继节点的入度
            for (Node next : cur.nextNodes) {
                Integer old = nodeInMap.put(next, nodeInMap.get(next) - 1);
                if (old == 1) zeroInNode.addLast(next);
            }
        }
        // 3.只要节点数量不对，肯定是因为有向环导致入度不为0
        if (result.size() != graph.nodes.size()) {
            throw new RuntimeException("拓扑排序发现出现了有向环");
        }
        return result;
    }
}
