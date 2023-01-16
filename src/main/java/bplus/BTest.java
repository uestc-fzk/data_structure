package bplus;

import org.junit.jupiter.api.Test;
import util.MyRandomUtil;

import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author fzk
 * @datetime 2023-01-16 00:13:01
 */
public class BTest {
    public static void main(String[] args) {
        new BTest().test1();
    }

    //    @Test
    void test1() {
        BTree bPlusTree = new BTree(16);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        HashSet<String> set = new HashSet<>();
        String key = null;
        for (int i = 0; i < 1000; i++) {
            key = MyRandomUtil.randomLowerStr(4);
//            String key = String.format("1%04d", random.nextInt(0, 10000));
            if (!set.add(key)) {
                System.out.println("重复key: " + key);
            }
//            System.out.println(key);
            bPlusTree.insert(key, i);
//            bPlusTree.root.printSelf();
//            System.out.println();
        }

        bPlusTree.root.printSelf();
        System.out.println(set.size());
        bPlusTree.root.checkSelf();

        set.forEach(k -> {
            if (bPlusTree.get(k) == null) {
                System.out.println("???不存在key: " + k);
            }
        });
    }

    @Test
    void test2() {
        BTree b = new BTree(4);
        HashSet<String> set = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            String key = MyRandomUtil.randomLowerStr(4);
            if (!set.add(key)) {
                System.out.println("重复key: " + key);
            }
            b.insert(key, i);
        }
        b.printSelf();
        for (String key : set) {
            if (!b.remove(key)) {
                System.out.println("删除失败：" + key);
            }
            b.printSelf();
            b.checkSelf();
        }
    }
}
