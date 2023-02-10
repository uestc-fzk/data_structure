package com.fzk.graph.minTree;

/**
 * 并查集
 *
 * @author fzk
 * @datetime 2022-08-19 17:05
 */
public class UnionFind {
    private int[] id;// 分量id（以触点作为索引）
    private int count;// 分量数量

    public UnionFind(int N) {
        // 初始化分量id数组
        count = N;
        id = new int[N];
        for (int i = 0; i < N; i++)
            id[i] = i;
    }

    public int getCount() {
        return this.count;
    }

    public boolean connected(int p, int q) {
        return find(p) == find(q);
    }

    public int find(int p) {
//            todo
        return 0;
    }

    public void union(int p, int q) {

    }

    public static void main(String[] args) {
        int[][] arr = new int[][]{
                {1, 2},
                {2, 3},
                {3, 4},
                {4, 5},
                {5, 1},
                {6, 7},
                {7, 8},
                {8, 9},
                {9, 10}
        };
        UnionFind unionFind = new UnionFind(10);
    }
}
