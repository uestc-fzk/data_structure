package com.fzk.bplus;

import java.util.LinkedList;
import java.util.Queue;

/**
 * B+树结点接口
 *
 * @author fzk
 * @datetime 2023-01-14 23:53:45
 */
public abstract class BNode {
    public int size = 0;// key的数量
    private static final int BinarySearchThreshold = 8;// 二分查找阈值，数组较小时遍历平均情况下会比较次数更少
    public BEntry[] entries;
    public BNode parentNode;

    public String getMinKey() {
        return entries[0].key;
    }

    public String getMaxKey() {
        return entries[size - 1].key;
    }

    public boolean isFull() {
        return size == entries.length;
    }

    public boolean isExists(String key) {
        return searchKeyIndex(key) != -1;
    }

    // 查找key的索引，不存在返回-1
    public int searchKeyIndex(String key) {
        if (size <= 0) return -1;
        if (size >= BinarySearchThreshold) {
            int left = 0, right = size - 1;
            while (left <= right) {
                int mid = (left + right) >> 1;
                int cmp = key.compareTo(entries[mid].key);
                if (cmp < 0) right = mid - 1;
                else if (cmp == 0) return mid;
                else left = mid + 1;
            }
        } else {
            // 遍历
            for (int i = 0; i < size; i++) {
                if (key.compareTo(entries[i].key) == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    // 更新key的value，必须已经存在
    public Object update(String key, Object newVal) {
        int index = searchKeyIndex(key);
        if (index == -1)
            throw new RuntimeException(String.format("更新key: %s的value时在待替换结点中未找到该key", key));
        Object old = entries[index].value;
        entries[index].value = newVal;
        return old;
    }

    /**
     * node结点minKey发生变化时，循环修改node结点的祖先节点的key
     *
     * @param newMinKey 变后的min key
     * @param oldMinKey 变前的min key
     */
    public void dfsUpdateMinKey(String newMinKey, String oldMinKey) {
        BNode p = this.parentNode;
        while (p != null) {
            int i = p.searchKeyIndex(oldMinKey);
            myAssert(i != -1, String.format("min key为%s的结点的父节点中没找到该key", oldMinKey));

            p.entries[i].key = newMinKey;
            // 在父节点中也是min key，循环向上修改
            if (i == 0) p = p.parentNode;
            else break;// 父节点中不是minKey，修改到此结束
        }
    }

    // 插入key
    public void insertKey(String key, Object value) {
        // 如果插入key比当前结点minKey都小，则需要循环维护指向此节点的父节点的key（指向此节点的key必须是结点最小key）
        if (this.size > 0 && key.compareTo(getMinKey()) < 0 && this.parentNode != null) {
            dfsUpdateMinKey(key, getMinKey());
        }

        // 插入排序咯
        int i = size;// 待插入索引
        while (i > 0 && key.compareTo(entries[i - 1].key) < 0) {
            entries[i] = entries[i - 1];
            i--;
        }
        entries[i] = new BEntry(key, value);
        size++;
        // 如果插入的value是结点，则必须维护父子结点关系
        if (value instanceof BNode vn) {
            vn.parentNode = this;
        }
    }

    public void clear() {
        parentNode = null;// 置空避免内存泄露
        entries = null;
    }

    // 每个结点维护一组键值映射
    public static class BEntry {
        public String key;
        public Object value;// 在叶子结点中为保存的值，在非叶子结点中保存为该key指向的childNode

        public BEntry(String key, Object value) {
            this.key = key;
            this.value = value;
        }
    }

    // 打印B+树
    public void printSelf() {
        Queue<BNode> queue = new LinkedList<>();
        queue.add(this);
        while (queue.size() > 0) {
            // 这一层个数
            int len = queue.size();
            while (len-- > 0) {
                BNode remove = queue.remove();
                System.out.print(" |");
                for (int i = 0; i < remove.size; i++) {
                    if (i == remove.size - 1)
                        System.out.printf("%s", remove.entries[i].key);
                    else System.out.printf("%s ", remove.entries[i].key);
                    if (remove.entries[i].value instanceof BNode) {
                        queue.add((BNode) remove.entries[i].value);
                    }
                }
                System.out.print("| ");
            }
            System.out.println();
        }
    }

    public static void myAssert(boolean flag, String msg) {
        if (!flag) throw new RuntimeException(msg);
    }
}