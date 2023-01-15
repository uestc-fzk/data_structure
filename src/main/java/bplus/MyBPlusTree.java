package bplus;

/**
 * 自定义实现B+树
 * B树中所有结点的孩子个数的最大值称为B树的阶, 一般从查找效率考虑，通常要求M>=3
 * 每一个节点最多M个子节点，子节点与key数量相同
 * key是唯一，即唯一性索引
 *
 * @author fzk
 * @datetime 2023-01-14 23:25:41
 */
public class MyBPlusTree {
    // B树的阶
    public final int M;
    public BNode root;// 根结点
    public LeafNode head;// 叶子结点链表头结点

    public MyBPlusTree(int m) {
        if (m < 4) throw new RuntimeException("B+树的阶不能小于4");
        M = m;
    }

    // 插入或更新，返回可能存在的旧值
    public Object insert(String key, Object value) {
        // 根结点为空，说明的第一次插入，需更新root和head
        if (root == null) {
            root = new LeafNode(M);
            root.insertKey(key, value);
            head = (LeafNode) root;
            return null;
        }

        // 如果root是叶子结点，直接插入
        if (root instanceof LeafNode) {
            if (!(root == head && root.parentNode == null)) {
                throw new RuntimeException("root是叶子结点却不等于head或其父节点不为null?");
            }
            return insertNode(root, key, value);
        }

        // 从root开始找到key应该插入的叶子结点
        LeafNode toInsertLeaf = findLeafNodeByKey((NonLeafNode) root, key);
        return insertNode(toInsertLeaf, key, value);
    }

    private Object insertNode(BNode node, String key, Object value) {
//        // node.getMinKey()<=key必然成立
//        if (!(node.getMinKey().compareTo(key) <= 0)) {
//            throw new RuntimeException(String.format("node minKey <= key: %s没成立?", key));
//        }

        // key存在，则覆盖
        if (node.isExists(key)) {
            // 如果已经存在，则覆盖原值，此时这里必须是叶子结点
            if (!(node instanceof LeafNode)) {
                throw new RuntimeException(String.format("即将发生key:%s 替换的结点居然不是叶子结点?", key));
            }
            // 覆盖值
            return node.replace(key, value);
        }

        BNode toInsert = node;
        // 新key，则插入
        // 如果结点已满，先进行页分裂
        if (node.isFull()) {
            // mmp，页分裂，向右分裂一半
            // todo: 这里需要根据即将插入的key来进行更优化的页分裂
            BNode rightNode = (node instanceof LeafNode) ? new LeafNode(M) : new NonLeafNode(M);
            int partitionIndex = node.size / 2;
            System.arraycopy(node.entries, partitionIndex, rightNode.entries, 0, node.size - partitionIndex);
            for (int i = partitionIndex; i < node.size; i++) {
                node.entries[i] = null;
            }
            // 更新2个节点的size
            rightNode.size = node.size - partitionIndex;
            node.size = partitionIndex;

            // 叶子结点需维护链表
            if (node instanceof LeafNode ln) {
                LeafNode lr = (LeafNode) rightNode;
                lr.preNode = ln;
                lr.nextNode = ln.nextNode;
                ln.nextNode = lr;
            }
            // 非叶子结点需维护子结点的父节点
            else {
                for (int i = 0; i < rightNode.size; i++) {
                    BNode childNode = (BNode) rightNode.entries[i].value;
                    childNode.parentNode = rightNode;
                }
            }

            // 将新的分裂right结点插入其父节点中
            // 新节点的最小key向上传递
            if (node.parentNode == null) {
                // 说明此时node为root
                if (!(node == root)) {
                    throw new RuntimeException("node的父节点为null却不是root?");
                }

                // 必须新建父节点作为root
                NonLeafNode parent = new NonLeafNode(M);
                // 父节点维护子结点
                // 将node和rightNode作为子结点插入到新建父节点中
                parent.insertKey(node.getMinKey(), node);
                parent.insertKey(rightNode.getMinKey(), rightNode);
                root = parent;
//                // 子结点维护父节点
//                node.parentNode = root;
//                rightNode.parentNode = root;
            } else {
//                rightNode.parentNode = node.parentNode;
                insertNode(node.parentNode,rightNode.getMinKey(), rightNode);
            }

            // 页分裂完成，选择插入结点
            if (key.compareTo(rightNode.getMinKey()) >= 0) {
                toInsert = rightNode;
            }
        }
        // 此时结点必然未满，直接插入
        toInsert.insertKey(key, value);
        return null;
    }

    // 找到key所在的叶子结点
    private LeafNode findLeafNodeByKey(NonLeafNode from, String key) {
        NonLeafNode node = from;
        outer:
        while (node != null) {
            // 如果key比node最小key都小，则最小key指向子结点为正确路径下一层级
            if (key.compareTo(node.getMinKey()) < 0) {
                BNode.BEntry entry = node.entries[0];
                // 如果指向的是叶子结点，直接返回key所在叶子结点
                if (entry.value instanceof LeafNode) {
                    return (LeafNode) entry.value;
                }
                // 否则是非叶子结点
                node = (NonLeafNode) entry.value;
                continue outer;
            }
            inner:
            for (int i = 0; i < node.size; i++) {
                // k1<= key <k2，则k1指向的子结点为正确路径的下一层级
                if (node.entries[i].key.compareTo(key) <= 0) {
                    if (i == node.size - 1 || node.entries[i + 1].key.compareTo(key) > 0) {
                        BNode.BEntry entry = node.entries[i];
                        // 如果指向的是叶子结点，直接返回key所在叶子结点
                        if (entry.value instanceof LeafNode) {
                            return (LeafNode) entry.value;
                        }
                        // 否则是非叶子结点
                        node = (NonLeafNode) entry.value;
                        break inner;
                    }
                } else throw new RuntimeException(String.format("%s 比node最小key小?", key));
            }
        }
        throw new RuntimeException(String.format("%s 没找到叶子结点", key));
    }

    // 非叶子结点
    static class NonLeafNode extends BNode {
        public NonLeafNode(int m) {
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
    }

}
