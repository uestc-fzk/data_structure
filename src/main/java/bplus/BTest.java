package bplus;

import util.MyRandomUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author fzk
 * @datetime 2023-01-16 00:13:01
 */
public class BTest {
    public static void main(String[] args) {
        BTest t = new BTest();
        for (int i = 0; i < 100; i++) {
            t.test1();
        }
        t.test3();
    }

    // 测试字符串随机插入和随机删除
    void test1() {
        BTree b = new BTree(16);
        HashSet<String> set = new HashSet<>();
        String key = null;
        for (int i = 0; i < 1000; i++) {
            key = MyRandomUtil.randomLowerStr(4);
            if (!set.add(key)) {
                System.out.println("重复key: " + key);
            }
            b.insert(key, i);
            b.checkTree();
        }

        b.printTree();
        System.out.println(set.size());

        set.forEach(k -> {
            if (!b.remove(k)) {
                throw new RuntimeException(String.format("%s删除失败", k));
            }
            b.checkTree();
        });
    }

    void test2() {
        BTree b = new BTree(4);
        HashSet<String> set = new HashSet<>();
        ArrayList<String> list = new ArrayList<>();
        try {
            for (int i = 0; i < 100; i++) {
                String key = String.format("%03d", ThreadLocalRandom.current().nextInt(0, 1000));
                list.add(key);
                if (!set.add(key)) {
                    System.out.println("重复key: " + key);
                }
                b.insert(key, i);
            }
            b.printTree();
            for (String key : set) {
                System.out.println("删除" + key);
                if (!b.remove(key)) {
                    System.out.println("删除失败：" + key);
                }
                b.printTree();
                b.checkTree();
            }
        } catch (RuntimeException e) {
            System.out.println("插入顺序:" + Arrays.toString(list.toArray()));
            throw e;
        }
    }


    void test3() {
        BTree b = new BTree(4);
        String msg = "180, 733, 406, 459, 408, 077, 404, 699, 427, 499, " +
                "433, 999, 484, 647, 920, 057, 482, 859, 357, 802, 724, " +
                "132, 848, 062, 841, 993, 645, 968, 168, 965, 378, 490, " +
                "480, 352, 509, 684, 844, 851, 321, 830, 034, 646, 446, " +
                "233, 842, 005, 373, 906, 525, 266, 838, 558, 696, 246, " +
                "897, 641, 944, 365, 137, 671, 918, 161, 848, 020, 866, " +
                "114, 470, 065, 603, 595, 946, 436, 387, 415, 532, 525, " +
                "303, 261, 676, 040, 873, 745, 678, 185, 303, 784, 297, " +
                "945, 221, 113, 493, 844, 003, 625, 196, 112, 270, 013, 531, 996";
        String[] splits = msg.split(",\\s+");
        HashSet<String> set = new HashSet<>();
        for (String key : splits) {
            if (!set.add(key)) {
                System.out.println("重复key: " + key);
            }
            System.out.println("插入key:" + key);
            b.insert(key, 0);
            b.checkTree();
        }

        b.checkTree();
        b.printTree();
        b.printLeaf();
        for (String key : set) {
            System.out.println("\n删除" + key);
            if (!b.remove(key)) {
                System.out.println("删除失败：" + key);
            }
            b.printTree();
            b.printLeaf();
            b.checkTree();
        }
    }
}
