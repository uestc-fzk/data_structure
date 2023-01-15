package graph;

import java.util.ArrayList;

/**
 * @author fzk
 * @datetime 2022-08-19 10:32
 */
public class Node {
    public int value;
    public int in = 0;// 入度
    public int out = 0;// 出度
    public ArrayList<Node> nextNodes;
    public ArrayList<Edge> edges;

    public Node(int value) {
        this.value = value;
        this.nextNodes = new ArrayList<>();
        this.edges = new ArrayList<>();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < this.nextNodes.size(); i++) {
            Node next = this.nextNodes.get(i);
            if (i != 0) sb.append(", ");
            sb.append(next.value);
        }
        sb.append(']');
        return "Node{" +
                "value=" + value +
                ", in=" + in +
                ", out=" + out +
                ", nextNodes=" + sb.toString() +
                '}';
    }
}
