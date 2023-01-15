package bplus;

import util.MyRandomUtil;

import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author fzk
 * @datetime 2023-01-15 00:02:05
 */
public class Main {
    public static void main(String[] args) {
        MyBPlusTree bPlusTree = new MyBPlusTree(16);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        HashSet<String> set = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String key= MyRandomUtil.randomLowerStr(4);
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
//        bPlusTree.root.printSelf();
    }
}
