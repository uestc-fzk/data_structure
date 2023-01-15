package graph;

import java.util.*;

/**
 * @author fzk
 * @datetime 2022-08-19 10:52
 */
public class MyGraphTest {
    public static void main(String[] args) {
        int[][] matrix = new int[][]{
                {1, 2, 0},
                {2, 3, 0},
                {3, 4, 0},
                {4, 5, 0},
                {1, 6, 0},
                {1, 7, 0},
                {2, 8, 0},
                {3, 9, 0},
                {7, 10, 0},
                {11, 10, 0},
                {10, 1, 0},
        };
        MyGraph graph = new MyGraph(matrix);

        ArrayList<Node> bfs = bfs(graph);
        System.out.println("bfs...");
        for (Node node : bfs) {
            System.out.println(node);
        }
        System.out.println("bfs...");


        ArrayList<Node> dfs = dfs(graph);
        System.out.println("dfs...");
        for (Node df : dfs) {
            System.out.println(df);
        }
        System.out.println("dfs...");

        List<Node> cycle = DirectedCycle.getCycle(graph);
        if (cycle != null) {
            System.out.println("有向环：" + TopoSort.isHasCycle(graph));
            for (Node node : cycle) {
                System.out.println(node);
            }
            System.out.println("有向环结束...");
        }

        ArrayList<Node> topoSort = TopoSort.topoSort(graph);
        for (Node node : topoSort) {
            System.out.println(node);
        }
    }

    public static ArrayList<Node> bfs(MyGraph graph) {
        ArrayList<Node> result = new ArrayList<>();
        HashSet<Node> accessed = new HashSet<>();// 已经访问过的标记
        LinkedList<Node> queue = new LinkedList<>();

        for (Node node : graph.nodes.values()) {
            if (!accessed.contains(node))
                bfs(node, accessed, queue, result);
        }
        return result;
    }

    private static void bfs(Node node, HashSet<Node> accessed, LinkedList<Node> queue, ArrayList<Node> result) {
        if (node == null) return;

        queue.addLast(node);
        accessed.add(node);
        result.add(node);
        while (queue.size() > 0) {
            Node cur = queue.removeFirst();
            for (Node next : cur.nextNodes) {
                if (!accessed.contains(next)) {
                    accessed.add(next);
                    queue.addLast(next);

                    // ---这里执行bfs操作
                    result.add(next);
                    // ---
                }
            }
        }
    }

    public static ArrayList<Node> dfs(MyGraph graph) {
        HashSet<Node> accessed = new HashSet<>();
        ArrayList<Node> result = new ArrayList<>();
        LinkedList<Node> stack = new LinkedList<>();

        for (Node node : graph.nodes.values()) {
            if (!accessed.contains(node))
                dfs(node, accessed, result, stack);
        }
        return result;
    }

    private static void dfs(Node node, HashSet<Node> accessed, ArrayList<Node> result, LinkedList<Node> stack) {
        if (node == null) return;

        stack.addLast(node);
        accessed.add(node);
        result.add(node);
        while (stack.size() > 0) {
            Node cur = stack.removeLast();
            for (Node next : cur.nextNodes) {
                if (!accessed.contains(next)) {
                    accessed.add(next);

                    // 这里再将此元素加入栈中，再最后进行break是为了完美的切合dfs访问节点顺序流程
                    stack.addLast(cur);
                    stack.addLast(next);

                    // ---这里执行dfs操作
                    result.add(next);
                    // ---

                    break;// break目的是为了完美切合dfs流程
                }
            }
        }
    }


}
