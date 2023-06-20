package com.fzk.bplus;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 自定义实现B+树
 * B树中所有结点的孩子个数的最大值称为B树的阶, 一般从查找效率考虑，通常要求M>=3
 * 每一个节点最多M个子节点，子节点与key数量相同
 * key是唯一，即唯一性索引
 * <a href="https://segmentfault.com/a/1190000041696709">B+树原理以及Go语言实现</a>
 * 注意：该文章的理论可借鉴，实现有问题
 *
 * @author fzk
 * @datetime 2023-01-14 23:25:41
 */
public class BTree {
    // B+树的阶
    public final int M;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private BNode root;// 根结点
    private LeafNode head;// 叶子结点链表头结点

    public BTree(int m) {
        if (m < 4) throw new RuntimeException("B+树的阶不能小于4");
        M = m;
    }

    /**
     * 插入或更新指定key的值
     *
     * @return 若key不存在则null，存在则返回旧值
     */
    public synchronized Object put(String key, Object value) {
        lock.writeLock().lock();
        try {
            // 根结点为空，说明的第一次插入，需更新root和head
            if (root == null) {
                assert head == null : "root为null但head不为null";
                root = new LeafNode(M);
                root.insert(key, value);
                head = (LeafNode) root;
                return null;
            }

            // 如果root是叶子结点，直接插入
            if (root instanceof LeafNode) {
                assert root == head && root.parentNode == null : "root是叶子结点却不等于head或其父节点不为null";
                return insertNode(root, key, value);
            }

            // 从root开始找到key应该插入的叶子结点
            LeafNode toInsertLeaf = findLeafNodeByKey(root, key);
            return insertNode(toInsertLeaf, key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 返回指定key的值
     *
     * @return null或值
     */
    public synchronized Object get(String key) {
        lock.readLock().lock();
        try {
            if (root == null || key.compareTo(root.getMinKey()) < 0) return null;
            LeafNode leafNode = findLeafNodeByKey(root, key);
            BNode.SearchKeyResult keyResult = leafNode.searchKeyIndex(key);
            if (keyResult.exists) {
                return leafNode.entries[keyResult.index].value;
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 删除指定key
     *
     * @return key不存在返回null，存在时返回删除的旧值
     */
    public synchronized Object remove(String key) {
        lock.writeLock().lock();
        try {
            if (root == null || key.compareTo(root.getMinKey()) < 0) return null;
            LeafNode node = findLeafNodeByKey(root, key);
            return deleteKeyFromNode(node, key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // 删除节点上的key映射
    private Object deleteKeyFromNode(BNode node, String key) {
        // 1.查找key在node的索引
        BNode.SearchKeyResult keyResult = node.searchKeyIndex(key);
        if (!keyResult.exists) return null;
        int index = keyResult.index;
        Object result = node.entries[keyResult.index].value;
        // 2.key为此结点min key，且删除后结点为空，把结点也删了
        if (index == 0 && node.size == 1) {
            // 2.1 如果该节点是root，则直接清空b+树
            if (node == root) {
                clear();
                return result;
            }
            // 2.2 维护双向链表
            BNode pre = node.preNode, next = node.nextNode;
            if (pre != null) pre.nextNode = next;
            if (next != null) next.preNode = pre;
            if (head == node) head = (LeafNode) next;// 如果是删除头结点，修改为后继节点

            // 2.3 从父节点中删除指向此节点的key
            BNode parent = node.parentNode;
            node.clear();// 清空node避免内存泄露
            deleteKeyFromNode(parent, key);
            return result;
        }

        // 3.从结点删除key
        System.arraycopy(node.entries, index + 1, node.entries, index, node.size - index - 1);
        node.entries[--node.size] = null;// 置空避免内存泄漏

        // 4.key为此节点min key，需要循环修改祖先结点指向此节点的key
        if (index == 0) {
            String newMinKey = node.getMinKey();
            node.dfsUpdateMinKey(newMinKey, key);
        }

        // 4.node的子结点或元素数量减少，尝试合并node
        tryMergeNode(node);
        return true;
    }

    // 合并结点

    /**
     * 叶子结点需要合并的情况：
     * |509 906|
     * |509|  |906|
     * |509|  |906|
     * |509|  |906|
     * 索引结点需要合并的情况：
     * |427 944|
     * |427|  |944|
     * |427|  |944|
     * |427 509 625 906|  |944 945 946|
     * 合并索引结点：尽量向左合并，这样可以减少min key修改
     * 目的在于降低层高，每个索引结点仅持有少量的子结点时，可能出现叶子结点非常少的情况下，层高非常高
     * 是索引结点说明其某个子结点刚被删除
     *
     * @param node 叶子结点和旁边的合并，索引结点怎么合并呢？
     */
    private void tryMergeNode(BNode node) {
        if (node.size >= this.M / 2) return;
        BNode pre = node.preNode, next = node.nextNode;
        if (pre != null && (pre.size + node.size) <= M) {
            // 优先将node合入左节点
            doMergeNode(pre, node);
        } else if (next != null && (next.size + node.size) <= M) {
            // 再考虑将右节点合入node
            doMergeNode(node, next);
        } else if (pre == null && next == null) {
            // 没有兄弟节点，必须是root节点
            assert root == node : "没有兄弟节点必须的root节点";
            // 如果root节点页key数量为1，是索引页时，则循环向下降低层高，即删除没必要的索引页
            while (root.size == 1 && root instanceof IndexNode) {
                BNode child = (BNode) root.entries[0].value;
                child.parentNode = null;// root节点没有父节点
                root.clear();// 清空旧root节点
                root = child;
            }
        }
    }

    // 将right节点合入left节点，尽量都向左合并，这样可以不修改Head结点
    private void doMergeNode(BNode left, BNode right) {
        // 1.将right节点拷贝到left节点
        System.arraycopy(right.entries, 0, left.entries, left.size, right.size);
        left.size += right.size;
        // 如果是索引节点需要维护父子关系
        if (left instanceof IndexNode) {
            for (int i = 0; i < right.size; i++) {
                ((BNode) right.entries[i].value).parentNode = left;
            }
        }

        // 2.维护双向链表
        BNode next = right.nextNode;
        left.nextNode = next;
        if (next != null) next.preNode = left;

        // 3.删除节点
        deleteKeyFromNode(right.parentNode, right.getMinKey());
        right.clear();// 清空节点避免内存泄露
    }

    private Object insertNode(BNode node, String key, Object value) {
        // key存在，则覆盖
        if (node.isExists(key)) {
            // 如果已经存在，则覆盖原值，此时这里必须是叶子结点
            assert (node instanceof LeafNode) : String.format("即将发生key:%s 替换的结点居然不是叶子结点?", key);
            return node.update(key, value);
        }

        // 新key，则插入
        BNode toInsert = node;
        if (node.isFull()) {// mmp，页分裂，向右分裂一半
            // todo: 这里需要根据即将插入的key来进行更优化的页分裂
            BNode rightNode = splitNode(node);

            // 页分裂完成，选择插入结点
            if (key.compareTo(rightNode.getMinKey()) >= 0) {
                toInsert = rightNode;
            }
        }
        // 此时结点必然未满，直接插入
        toInsert.insert(key, value);
        return null;
    }

    // 结点向右分裂
    private BNode splitNode(BNode leftNode) {
        BNode rightNode;
        if (leftNode instanceof LeafNode) rightNode = new LeafNode(M);
        else rightNode = new IndexNode(M);

        // 将一半key拷贝到新结点
        int partitionIndex = leftNode.size >> 1;
        System.arraycopy(leftNode.entries, partitionIndex, rightNode.entries, 0, leftNode.size - partitionIndex);
        Arrays.fill(leftNode.entries, partitionIndex, leftNode.size, null);// 必须置null避免内存泄漏(删除key这里却有引用)
        // 更新2个节点的size
        rightNode.size = leftNode.size - partitionIndex;
        leftNode.size = partitionIndex;

        // 维护双向链表
        BNode next = leftNode.nextNode;
        leftNode.nextNode = rightNode;
        rightNode.preNode = leftNode;
        rightNode.nextNode = next;
        if (next != null) {
            next.preNode = rightNode;
        }

        // 索引结点需维护子结点的父节点
        if (leftNode instanceof IndexNode) {
            for (int i = 0; i < rightNode.size; i++) {
                BNode childNode = (BNode) rightNode.entries[i].value;
                childNode.parentNode = rightNode;
            }
        }

        // 将新结点插入其父节点中
        if (leftNode.parentNode == null) {
            // 说明此时node为root
            assert leftNode == root : "node的父节点为null却不是root";

            // 必须新建父节点作为root
            IndexNode parent = new IndexNode(M);
            // 父节点维护子结点
            // 将node和rightNode作为子结点插入到新建父节点中
            parent.insert(leftNode.getMinKey(), leftNode);
            parent.insert(rightNode.getMinKey(), rightNode);
            root = parent;
        } else insertNode(leftNode.parentNode, rightNode.getMinKey(), rightNode);
        return rightNode;
    }

    // 找到key所在或即将插入的叶子结点
    private LeafNode findLeafNodeByKey(BNode from, String key) {
        if (from instanceof LeafNode ln) return ln;

        BNode cur = from;
        while (cur instanceof IndexNode) {
            // k1 <= key < k2，则k1指向的子结点为正确路径的下一层级
            BNode.SearchKeyResult keyResult = cur.searchKeyIndex(key);
            int nextIndex;
            if (keyResult.exists) {
                nextIndex = keyResult.index;
            } else if (keyResult.index == 0) {
                nextIndex = 0;
            } else {
                // 不存在时，返回的是key应该插入的索引，则下个节点索引为index-1
                nextIndex = keyResult.index - 1;
            }
            BNode next = (BNode) cur.entries[nextIndex].value;
            // 如果指向的是叶子结点，直接返回key所在叶子结点
            if (next instanceof LeafNode leaf) {
                return leaf;
            }
            cur = next;
        }
        throw new RuntimeException(String.format("%s 没找到叶子结点", key));
    }

    public void printTree() {
        if (root == null) System.out.println("{}");
        else root.printSelf();
    }

    // 打印叶子结点双向链表
    public String getLeafString() {
        if (head == null) return "{}";
        // 先打印
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        BNode cur = head;
        while (cur != null) {
            sb.append(cur.getKeysString()).append(", ");
            cur = cur.nextNode;
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append('}');
        return sb.toString();
    }

    public void clear() {
        this.root = null;
        this.head = null;
    }

    // 非叶子结点，即索引结点
    static class IndexNode extends BNode {
        public IndexNode(int m) {
            this.entries = new BEntry[m];
        }
    }


    // 叶子结点
    static class LeafNode extends BNode {
        public LeafNode(int m) {
            this.entries = new BEntry[m];
        }
    }

    // 检查B+树结构是否正常，此操作会以bfs遍历整个树结构，操作代价高
    public void checkTree() {
        if (root == null) return;
        Queue<BNode> queue = new LinkedList<>();
        queue.add(root);
        while (queue.size() > 0) {
            BNode curLevelNode = queue.peek();// 此层当前遍历的节点，用于检查双向链表
            int len = queue.size();// 此层个数
            while (len-- > 0) {
                BNode remove = queue.remove();
                // 1.节点自检
                remove.checkSelf();
                // 2.索引结点：检查父子关系
                // 2.1 检查此节点的key指向的是子结点min key
                // 2.2 检查此节点和子结点的父子关系是否正常
                if (remove instanceof IndexNode ir) {
                    for (int i = 0; i < ir.size; i++) {
                        queue.add((BNode) ir.entries[i].value);
                        if (!ir.entries[i].key.equals(((BNode) ir.entries[i].value).getMinKey())) {
                            throw new RuntimeException(String.format("%s不是其指向的子结点min key", ir.entries[i].key));
                        }
                        if ((((BNode) ir.entries[i].value).parentNode != ir)) {
                            throw new RuntimeException(String.format("%s的父节点指向错误", ir.entries[i].key));
                        }
                    }
                }

                // 3.检查双向链表是否正常
                // 首先检查双向链表指向的节点顺序和索引树指向的节点顺序一致
                if (remove != curLevelNode) {
                    printTree();
                    throw new RuntimeException(String.format("min key 为%s的结点顺序不正常", remove.getMinKey()));
                }
                // 检查前后节点指向正确
                if (remove.nextNode != null && remove.nextNode.preNode != remove) {
                    printTree();
                    throw new RuntimeException(String.format("min key为%s的结点后继结点的前继居然不是自己?", remove.getMinKey()));
                }
                // 此节点maxKey<后继节点的minKey
                if (remove.nextNode != null && remove.getMaxKey().compareTo(remove.nextNode.getMinKey()) >= 0) {
                    System.err.printf("此节点： %s，后继节点: %s\n", remove.getKeysString(), remove.nextNode.getKeysString());
                    throw new RuntimeException("此节点maxKey>=后继节点的minKey");
                }
                curLevelNode = curLevelNode.nextNode;
            }
        }
        // 检查head是否指向正确
        if (!head.getMinKey().equals(root.getMinKey()) || head.preNode != null) {
            throw new RuntimeException("head节点错误");
        }
    }
}
