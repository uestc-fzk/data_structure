package bplus;

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
        for (int i = 0; i < size; i++) {
            if (entries[i].key.equals(key))
                return true;
        }
        return false;
    }

    // 替换key，必须已经存在
    public Object replace(String key, Object newVal) {
        // todo: 这里可以优化为二分查找
        for (int i = 0; i < size; i++) {
            if (entries[i].key.equals(key)) {
                Object oldVal = entries[i].value;
                entries[i].value = newVal;
                return oldVal;
            }
        }
        throw new RuntimeException(String.format("%s没找到进行替换?", key));
    }


    // 插入key
    public void insertKey(String key, Object value) {
        // 如果插入key比当前结点minKey都小，则需要循环维护指向此节点的父节点的key（指向此节点的key必须是结点最小key）
        if (this.size > 0 && key.compareTo(getMinKey()) < 0 && this.parentNode != null) {
            BNode p = this.parentNode;
            outer:
            while (p != null) {
                inner:
                for (int i = 0; i < p.size; i++) {
                    if (getMinKey().equals(p.entries[i].key)) {
                        p.entries[i].key = key;
                        // 在父节点中也是minKey，循环向上修改
                        if (i == 0) {
                            p = p.parentNode;
                            continue outer;
                        } else {
                            // 父节点中不是minKey，修改到此结束
                            break outer;
                        }
                    }
                }
                throw new RuntimeException(String.format("父节点中没找到min key: %s", getMinKey()));
            }
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

    public static class BEntry {
        public String key;
        public Object value;

        public BEntry(String key, Object value) {
            this.key = key;
            this.value = value;
        }
    }

    // 打印b+树
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

    // 检查b+树是否合法
    public void checkSelf() {
        Queue<BNode> queue = new LinkedList<>();
        queue.add(this);
        while (queue.size() > 0) {
            int len = queue.size();// 此层个数
            while (len-- > 0) {
                BNode remove = queue.remove();
                // 先检查此节点的key是顺序排列的
                for (int i = 1; i < remove.size; i++) {
                    if (remove.entries[i - 1].key.compareTo(remove.entries[i].key) >= 0) {
                        printSelf();
                        throw new RuntimeException("存在结点的key不是顺序排列");
                    }
                }
                // 再检查此节点的key指向的是子结点min key
                if (remove instanceof MyBPlusTree.NonLeafNode) {
                    for (int i = 0; i < remove.size; i++) {
                        if (!remove.entries[i].key.equals(((BNode) remove.entries[i].value).getMinKey())) {
                            printSelf();
                            throw new RuntimeException(String.format("%s不是其指向的子结点min key", remove.entries[i].key));
                        }
                    }
                }
            }
        }
        System.out.println("B+树结构正常");
    }
}
