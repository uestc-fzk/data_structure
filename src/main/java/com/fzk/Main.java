package com.fzk;

import com.fzk.log.Logger;
import com.fzk.virtualtThread.VirtualTest;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
//        Logger.warning("1");
//        Logger.info("1");
//        Thread.sleep(1000);
//        testLog();
//        test1();
//        testLock();

        VirtualTest virtualTest = new VirtualTest();
//        virtualTest.testSleep();
        virtualTest.testIO();
    }

    static void testLog() {
        for (int i = 0; i < 10000; i++) {
            String content = i + "";
            Logger.fine(content);
            Logger.debug(content);
            Logger.info(content);
            Logger.warning(content);
            Logger.error(content);
            Logger.fatal(content);
        }
    }

    static void test1() {
        Instant now = Instant.now();
        String str = now.getEpochSecond() + "" + now.getNano();
        long aLong = Long.parseLong(str);
        System.out.println(aLong);
        System.out.println(Long.MAX_VALUE);

        System.out.println(ZoneOffset.getAvailableZoneIds().contains(ZoneId.systemDefault().getId()));
//        System.out.println(ZoneOffset.of(ZoneId.systemDefault().getId(),null ));
        Instant instant = Instant.ofEpochSecond(now.getEpochSecond(), now.getNano());
        System.out.println(instant);
        System.out.println(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));


        System.out.printf("%-8s", "warning");
    }

    static long nanoToSecond = 1000_000_000L;

    // java中出现死锁无法自动走出来
    static void testLock() {
        final ReentrantLock lockA = new ReentrantLock();
        final ReentrantLock lockB = new ReentrantLock();
        new Thread(() -> {
            synchronized (lockB) {
                System.out.println("2获取锁B");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                synchronized (lockA) {
                    System.out.println("2获取锁A");
                }
            }
        }).start();
        synchronized (lockA) {

            System.out.println("1获取锁A");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            synchronized (lockB) {
                System.out.println("1获取锁B");
            }
        }
    }
}