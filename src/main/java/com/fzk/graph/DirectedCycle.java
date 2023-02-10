package com.fzk.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * 此类用于寻找有向图的环，
 * 关键在于记录访问节点的前继节点，当出现环时从末尾节点一直往回退从而得到环
 *
 * @author fzk
 * @datetime 2022-08-19 14:51
 */
public class DirectedCycle {
    private final HashSet<Node> accessed;
    /**
     * 记录到达key结点的前继结点value
     * 如A-->B， 那么在这里key=B， value=A
     */
    private final HashMap<Node, Node> preNode;
    private final HashSet<Node> onStack;// 表示节点是否在dfs的调用栈上
    /**
     * 若为空则无环，不为空则必为如下形式
     * 有向环：[A,x,x,x,A]
     */
    public LinkedList<Node> cycle = null;

    public DirectedCycle(MyGraph graph) {
        int n = graph.nodes.size();
        accessed = new HashSet<>(n);
        preNode = new HashMap<>(n);
        onStack = new HashSet<>(n);
        for (Node node : graph.nodes.values()) {
            if (!accessed.contains(node)) {
                accessed.add(node);
                dfs(node);
            }
        }
    }

    private void dfs(Node node) {
        if (hasCycle()) return;
        onStack.add(node);// 模拟dfs入栈

        for (Node next : node.nextNodes) {
            if (hasCycle()) return;
            else if (!accessed.contains(next)) {
                accessed.add(next);
                preNode.put(next, node);
                dfs(next);
            }
            // 该后继节点在dfs调用栈上，说明出现有向环了
            else if (onStack.contains(next)) {
                cycle = new LinkedList<>();
                // 这里必须从node开始往回找前继结点
                for (Node x = node; x != next; x = preNode.get(x))
                    cycle.addFirst(x);
                cycle.addFirst(next);
                cycle.addFirst(node);// 再加入node形成闭环，此时环：[A,x,x,x,A]
            }
        }

        onStack.remove(node);// 模拟dfs出栈
    }

    public boolean hasCycle() {
        return cycle != null;
    }

    /**
     * 如有环则返回环，无环返回null
     */
    public static LinkedList<Node> getCycle(MyGraph graph) {
        DirectedCycle directedCycle = new DirectedCycle(graph);
        if (directedCycle.hasCycle()) return directedCycle.cycle;
        else return null;
    }
}
