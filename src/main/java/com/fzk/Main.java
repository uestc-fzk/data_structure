package com.fzk;

import com.fzk.log.Logger;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        testLog();
//        test1();
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


        System.out.printf("%-8s","warning");
    }

    static long nanoToSecond = 1000_000_000L;
}