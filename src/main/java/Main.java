import log.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        int count=50;
        CountDownLatch latch=new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            final int k = i;
            Thread t = new Thread(() -> {
                for (int j = 0; j < 10000; j++) {
                    Logger.info(String.valueOf(k + "-" + j));
                    Logger.fine("1111");
                    Logger.warning("222");
                }
                latch.countDown();
            });
            t.start();
        }
        Thread.sleep(1000);
        latch.await();

        BasicFileAttributes attributes = Files.readAttributes(Path.of("logs/info.log"), BasicFileAttributes.class);
        System.out.println(LocalDateTime.ofInstant(attributes.creationTime().toInstant(), ZoneId.systemDefault()));
        System.out.println(LocalDateTime.ofInstant(attributes.lastModifiedTime().toInstant(), ZoneId.systemDefault()));
        System.out.println(LocalDateTime.ofInstant(attributes.lastAccessTime().toInstant(), ZoneId.systemDefault()));
    }
}