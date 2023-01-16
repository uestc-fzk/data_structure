package bplus;

/**
 * 自定义实现B+树
 * B树中所有结点的孩子个数的最大值称为B树的阶, 一般从查找效率考虑，通常要求M>=3
 * 每一个节点最多M个子节点，子节点与key数量相同
 * key是唯一，即唯一性索引
 * <a href="https://segmentfault.com/a/1190000041696709">B+树原理以及Go语言实现</a>
 *
 * @author fzk
 * @datetime 2023-01-14 23:25:41
 */
public class BTree {
    // B树的阶
    public final int M;
    public BNode root;// 根结点
    public LeafNode head;// 叶子结点链表头结点

    public BTree(int m) {
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
            if (!(root == head && root.parentNode == null))
                throw new RuntimeException("root是叶子结点却不等于head或其父节点不为null?");
            return insertNode(root, key, value);
        }

        // 从root开始找到key应该插入的叶子结点
        LeafNode toInsertLeaf = findLeafNodeByKey(root, key);
        return insertNode(toInsertLeaf, key, value);
    }

    // 查询key指向元素
    public Object get(String key) {
        LeafNode leafNode = findLeafNodeByKey(root, key);
        int index = leafNode.searchKeyIndex(key);
        if (index != -1) {
            return leafNode.entries[index].value;
        }
        return null;
    }

    // 删除key指向元素
    public boolean remove(String key) {
        LeafNode node = findLeafNodeByKey(root, key);
        return deleteKeyFromNode(node, key);
    }

    private boolean deleteKeyFromNode(BNode node, String key) {
        int index = node.searchKeyIndex(key);
        if (index == -1) return false;
        else if (index == 0) {// key为此结点min key，需要循环修改祖先结点指向此节点的key
            // 若删除后结点为空，把结点也删了
            if (node.size == 1) {
                // 如果该节点是root，则直接清空b+树
                if (node == root) {
                    root = null;
                    head = null;
                    return true;
                }
                // 从父节点中删除指向此节点的key
                return deleteKeyFromNode(node.parentNode, node.getMinKey());
            } else {
                BNode.BEntry[] entries = node.entries;
                System.arraycopy(entries, index + 1, entries, index, node.size - index - 1);
                entries[--node.size] = null;// 置空避免内存泄漏

                // 需循环修改祖先结点的minKey
                String newMinKey = node.getMinKey();
                BNode parent = node.parentNode;
                while (parent != null) {
                    int pi = node.parentNode.searchKeyIndex(key);// key在父节点的索引0
                    parent.entries[pi].key = newMinKey;
                    if (pi == 0) { // 在父节点也是minKey，循环修改
                        parent = parent.parentNode;
                    } else break;
                }
            }
        } else {
            // 直接删除
            BNode.BEntry[] entries = node.entries;
            System.arraycopy(entries, index + 1, entries, index, node.size - index - 1);
            entries[--node.size] = null;// 置空避免内存泄漏
        }
        return true;
    }

    private Object insertNode(BNode node, String key, Object value) {
        // key存在，则覆盖
        if (node.isExists(key)) {
            // 如果已经存在，则覆盖原值，此时这里必须是叶子结点
            if (!(node instanceof LeafNode)) {
                throw new RuntimeException(String.format("即将发生key:%s 替换的结点居然不是叶子结点?", key));
            }
            // 覆盖值
            return node.replace(key, value);
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
        BNode rightNode = (leftNode instanceof LeafNode) ? new LeafNode(M) : new IndexNode(M);
        int partitionIndex = leftNode.size / 2;
        System.arraycopy(leftNode.entries, partitionIndex, rightNode.entries, 0, leftNode.size - partitionIndex);
        for (int i = partitionIndex; i < leftNode.size; i++) {
            leftNode.entries[i] = null;// 必须置null避免内存泄漏(删除key这里却有引用)
        }
        // 更新2个节点的size
        rightNode.size = leftNode.size - partitionIndex;
        leftNode.size = partitionIndex;

        // 叶子结点需维护链表
        if (leftNode instanceof LeafNode ln) {
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
        if (leftNode.parentNode == null) {
            // 说明此时node为root
            if (!(leftNode == root))
                throw new RuntimeException("node的父节点为null却不是root?");

            // 必须新建父节点作为root
            IndexNode parent = new IndexNode(M);
            // 父节点维护子结点
            // 将node和rightNode作为子结点插入到新建父节点中
            parent.insertKey(leftNode.getMinKey(), leftNode);
            parent.insertKey(rightNode.getMinKey(), rightNode);
            root = parent;
        } else {
            insertNode(leftNode.parentNode, rightNode.getMinKey(), rightNode);
        }
        return rightNode;
    }

    // 找到key所在或即将插入的叶子结点
    private LeafNode findLeafNodeByKey(BNode from, String key) {
        if (from instanceof LeafNode ln)
            return ln;

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

    public void printSelf(){
        if(root==null) System.out.println("null");
        else root.printSelf();
    }

    public void checkSelf(){
        if(root!=null){
            root.checkSelf();
        }
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
    }

}
