package bplus;

import java.util.LinkedList;
import java.util.Queue;

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
    public Object insert(String key, Object value) {
        // 根结点为空，说明的第一次插入，需更新root和head
        if (root == null) {
            myAssert(head == null, "root为null但head不为null");
            root = new LeafNode(M);
            root.insertKey(key, value);
            head = (LeafNode) root;
            return null;
        }

        // 如果root是叶子结点，直接插入
        if (root instanceof LeafNode) {
            myAssert(root == head && root.parentNode == null, "root是叶子结点却不等于head或其父节点不为null");
            return insertNode(root, key, value);
        }

        // 从root开始找到key应该插入的叶子结点
        LeafNode toInsertLeaf = findLeafNodeByKey(root, key);
        return insertNode(toInsertLeaf, key, value);
    }

    /**
     * 返回指定key的值
     *
     * @return null或值
     */
    public Object get(String key) {
        if (root == null || key.compareTo(root.getMinKey()) < 0) return null;
        LeafNode leafNode = findLeafNodeByKey(root, key);
        int index = leafNode.searchKeyIndex(key);
        if (index != -1) {
            return leafNode.entries[index].value;
        }
        return null;
    }

    /**
     * 删除指定key
     *
     * @return key不存在则为false，否则为true
     */
    public boolean remove(String key) {
        if (root == null || key.compareTo(root.getMinKey()) < 0) return false;
        LeafNode node = findLeafNodeByKey(root, key);
        return deleteKeyFromNode(node, key);
    }

    // 删除节点上的key映射
    private boolean deleteKeyFromNode(BNode node, String key) {
        // 1.查找key在node的索引
        int index = node.searchKeyIndex(key);
        if (index == -1) return false;
        // 2.key为此结点min key，且删除后结点为空，把结点也删了
        if (index == 0 && node.size == 1) {
            // 2.1 如果该节点是root，则直接清空b+树
            if (node == root) {
                clear();
                return true;
            }
            // 2.2 如果该结点是叶子结点，需维护链表
            if (node instanceof LeafNode ln) {
                LeafNode pre = ln.preNode;
                LeafNode next = ln.nextNode;
                if (pre != null) pre.nextNode = next;
                if (next != null) next.preNode = pre;
                if (head == node) head = next;// 如果是删除头结点，修改为后继节点
            }
            //2.3 从父节点中删除指向此节点的key
            BNode parent = node.parentNode;
            node.clear();// 清空node避免内存泄露
            return deleteKeyFromNode(parent, key);
        }
        // 3.从结点删除key
        BNode.BEntry[] entries = node.entries;
        System.arraycopy(entries, index + 1, entries, index, node.size - index - 1);
        entries[--node.size] = null;// 置空避免内存泄漏

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
     *
     * @param node 叶子结点和旁边的合并，索引结点怎么合并呢？
     */
    private void tryMergeNode(BNode node) {
        if (node.size >= this.M / 2) return;
        // 1.合并叶子结点
        if (node instanceof LeafNode ln) tryMergeLeaf(ln);
            // 2.合并索引结点
        else tryMergeIndex((IndexNode) node);
    }

    // 合并叶子结点：尽量都向左合并，这样可以不修改Head结点
    private void tryMergeLeaf(LeafNode ln) {
        LeafNode pre = ln.preNode;
        LeafNode next = ln.nextNode;
        if (pre != null && (pre.size + ln.size) <= M) {
            // 优先将leaf合入左结点
            mergeLeaf(pre, ln);
        } else if (next != null && (ln.size + next.size) <= M) {
            // 再考虑将右结点合入leaf
            mergeLeaf(ln, next);
        } else if (pre == null && next == null) {
            myAssert(head == ln, String.format("min key为%s的叶子结点左右结点都为null却不是head?", ln.getMinKey()));
            // 仅1个叶子结点，则将其置为root，舍弃所有索引结点，从而将层高降为1
            if (ln.parentNode != null) {
                ln.parentNode = null;// 必须置空避免内存泄露，并保证root无父节点
                root = ln;
            } else if (ln != root) throw new RuntimeException("无父节点必须为root");
        }
    }

    /**
     * 合并索引结点：尽量向左合并，这样可以减少min key修改
     * 目的在于降低层高，每个索引结点仅持有少量的子结点时，可能出现叶子结点非常少的情况下，层高非常高
     * 是索引结点说明其某个子结点刚被删除
     */
    private void tryMergeIndex(IndexNode in) {
        // 2.1 root结点：root结点只有在size=1时且为索引结点才能降层高
        if (in.parentNode == null) {
            myAssert(in == root, "无父节点必须为root");
            while (root.size == 1 && root instanceof IndexNode) {
                BNode nextRoot = (BNode) root.entries[0].value;
                nextRoot.parentNode = null;// 必须置空，避免内存泄露并时刻保证root父节点为null
                root = nextRoot;
            }
        } else {
            // 2.2 普通索引结点：寻找其父节点下的兄弟结点进行合并
            IndexNode parent = (IndexNode) in.parentNode;
            myAssert(parent.isExists(in.getMinKey()), String.format("min key为%s的父节点含有子结点个数为%d, 父节点中存在该key:%b?", in.getMinKey(), parent.size, parent.isExists(in.getMinKey())));

            // 无兄弟结点则先合并父节点
            if (parent.size == 1) {
                tryMergeNode(parent);
                tryMergeNode(in);// 先合并父节点，再合并自己，理论上此节点不会被删除
            } else {
                // 与兄弟结点合并
                int index = parent.searchKeyIndex(in.getMinKey());
                IndexNode left = null, right = null;
                if (index > 0) left = (IndexNode) parent.entries[index - 1].value;
                if (index + 1 < parent.size) right = (IndexNode) parent.entries[index + 1].value;
                if (left != null && left.size + in.size <= M) {
                    mergeIndex(left, in);
                } else if (right != null && right.size + in.size <= M) {
                    mergeIndex(in, right);
                }
                // 此时父节点的子结点减少，尝试合并父节点
                // 注意：这里绝对不能合并，因为mergeIndex()方法已经尝试合并父节点，此父节点很可能已经删除
                // tryMergeNode(parent);
            }
        }
    }

    private void mergeIndex(IndexNode node1, IndexNode node2) {
        // 1.将node2的key拷贝到node1
        System.arraycopy(node2.entries, 0, node1.entries, node1.size, node2.size);
        node1.size += node2.size;

        // 2.维护父子结点关系
        for (int i = 0; i < node2.size; i++) {
            ((BNode) node2.entries[i].value).parentNode = node1;
        }
        // 3.删除node2索引结点
        deleteKeyFromNode(node2.parentNode, node2.getMinKey());
    }

    // 将node2合入node1
    private void mergeLeaf(LeafNode node1, LeafNode node2) {
        // 1.将node2的key拷贝到node1
        System.arraycopy(node2.entries, 0, node1.entries, node1.size, node2.size);
        node1.size += node2.size;
        // 2.维护双向链表
        LeafNode next = node2.nextNode;
        node1.nextNode = next;
        if (next != null) next.preNode = node1;
        // 3.删除node2结点: 此时必有父节点
        BNode parent = node2.parentNode;
        String minKey = node2.getMinKey();
        node2.preNode = null;
        node2.nextNode = null;
        deleteKeyFromNode(parent, minKey);
        // 4.合并完成后若此时仅剩node1
        if (node1.preNode == null && node1.nextNode == null) {
            node1.parentNode = null;// 必须置空，避免内存泄露且root父节点必须为null
            root = node1;// 仅有1个叶子结点，则将其置为root，舍弃所有索引结点，从而将层高降为1
            head = node1;
        }
    }

    private Object insertNode(BNode node, String key, Object value) {
        // key存在，则覆盖
        if (node.isExists(key)) {
            // 如果已经存在，则覆盖原值，此时这里必须是叶子结点
            myAssert(node instanceof LeafNode, String.format("即将发生key:%s 替换的结点居然不是叶子结点?", key));
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
        toInsert.insertKey(key, value);
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
        for (int i = partitionIndex; i < leftNode.size; i++) {
            leftNode.entries[i] = null;// 必须置null避免内存泄漏(删除key这里却有引用)
        }
        // 更新2个节点的size
        rightNode.size = leftNode.size - partitionIndex;
        leftNode.size = partitionIndex;

        // 叶子结点需维护链表
        if (leftNode instanceof LeafNode ll) {
            LeafNode lr = (LeafNode) rightNode;
            LeafNode next = ll.nextNode;
            lr.preNode = ll;
            ll.nextNode = lr;
            lr.nextNode = next;
            if (next != null) next.preNode = lr;
        }
        // 索引结点需维护子结点的父节点
        else {
            for (int i = 0; i < rightNode.size; i++) {
                BNode childNode = (BNode) rightNode.entries[i].value;
                childNode.parentNode = rightNode;
            }
        }

        // 将新结点插入其父节点中
        if (leftNode.parentNode == null) {
            // 说明此时node为root
            myAssert(leftNode == root, "node的父节点为null却不是root");

            // 必须新建父节点作为root
            IndexNode parent = new IndexNode(M);
            // 父节点维护子结点
            // 将node和rightNode作为子结点插入到新建父节点中
            parent.insertKey(leftNode.getMinKey(), leftNode);
            parent.insertKey(rightNode.getMinKey(), rightNode);
            root = parent;
        } else insertNode(leftNode.parentNode, rightNode.getMinKey(), rightNode);
        return rightNode;
    }

    // 找到key所在或即将插入的叶子结点
    private LeafNode findLeafNodeByKey(BNode from, String key) {
        if (from instanceof LeafNode ln) return ln;

        IndexNode node = (IndexNode) from;
        for (int i = node.size - 1; i >= 0; i--) {
            // k1<= key <k2，则k1指向的子结点为正确路径的下一层级
            // 走到i==0说明key比min key都小
            if (key.compareTo(node.entries[i].key) >= 0 || i == 0) {
                BNode next = (BNode) node.entries[i].value;
                // 如果指向的是叶子结点，直接返回key所在叶子结点
                if (next instanceof LeafNode leaf) {
                    return leaf;
                }
                // 否则是非叶子结点
                node = (IndexNode) next;
                i = next.size;// 重置i，因为后续有个i--
            }
        }
        throw new RuntimeException(String.format("%s 没找到叶子结点", key));
    }

    public void printTree() {
        if (root == null) System.out.println("null");
        else root.printSelf();
    }

    // 打印叶子结点双向链表
    public void printLeaf() {
        // 先打印
        LeafNode cur = head;
        System.out.println("叶子结点链表：");
        while (cur != null) {
            System.out.print(" |");
            for (int i = 0; i < cur.size; i++) {
                if (i == cur.size - 1) System.out.printf("%s", cur.entries[i].key);
                else System.out.printf("%s ", cur.entries[i].key);
            }
            System.out.print("| ");
            cur = cur.nextNode;
        }
        System.out.println();
    }

    // 检查B+树结构是否正常，此操作会以bfs遍历整个树结构，操作代价高
    public void checkTree() {
        if (root == null) return;
        Queue<BNode> queue = new LinkedList<>();
        queue.add(root);
        LeafNode curLeaf = head;
        while (queue.size() > 0) {
            int len = queue.size();// 此层个数
            while (len-- > 0) {
                BNode remove = queue.remove();
                // 1.先检查此节点的key是顺序排列的：k1<k2
                for (int i = 1; i < remove.size; i++) {
                    myAssert(remove.entries[i - 1].key.compareTo(remove.entries[i].key) < 0, "存在结点的key不是顺序排列");
                }
                // 2.索引结点：
                // 2.1 检查此节点的key指向的是子结点min key
                // 2.2 检查此节点和子结点的父子关系是否正常
                // 2.3 检查此节点的key指向的子结点max key小于下一个key
                if (remove instanceof IndexNode ir) {
                    for (int i = 0; i < ir.size; i++) {
                        queue.add((BNode) ir.entries[i].value);
                        myAssert(ir.entries[i].key.equals(((BNode) ir.entries[i].value).getMinKey()), String.format("%s不是其指向的子结点min key", ir.entries[i].key));
                        myAssert((((BNode) ir.entries[i].value).parentNode == ir), String.format("%s的父节点指向错误", ir.entries[i].key));
                        if (i < ir.size - 1) {
                            myAssert((((BNode) ir.entries[i].value).getMaxKey().compareTo(ir.entries[i + 1].key) < 0), String.format("%s指向的子结点max key大于下一节点min key", ir.entries[i].key));
                        }
                    }
                }

                // 3.叶子结点：检查叶子结点双向链表是否正常
                if (remove instanceof LeafNode lr) {
                    // 检查索引树指向的叶子结点和叶子结点链表顺序一致
                    if (lr != curLeaf) {
                        printTree();
                        printLeaf();
                        throw new RuntimeException(String.format("min key 为%s的叶子结点顺序不正常", lr.getMinKey()));
                    }
                    // 检查前后结点链接是否正常
                    if (curLeaf.nextNode != null && curLeaf.nextNode.preNode != curLeaf) {
                        printTree();
                        printLeaf();
                        throw new RuntimeException(String.format("min key为%s的叶子结点后继结点的前继居然不是自己?", curLeaf.getMinKey()));
                    }
                    curLeaf = curLeaf.nextNode;
                }
            }
        }
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
        public LeafNode preNode;
        public LeafNode nextNode;

        public LeafNode(int m) {
            this.entries = new BEntry[m];
        }

        @Override
        public void clear() {
            super.clear();
            this.preNode = null;
            this.nextNode = null;
        }
    }

    // 断言，开发时用于定位问题，正常情况下必须不能出错
    public static void myAssert(boolean flag, String msg) {
        if (!flag) throw new RuntimeException(msg);
    }
}
