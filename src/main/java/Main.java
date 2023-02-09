import log.Logger;

import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int count=10;
        CountDownLatch latch=new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            final int k = i;
            Thread t = new Thread(() -> {
                for (int j = 0; j < 3000; j++) {
                    Logger.info(String.valueOf(k + "-" + j));
                }
                latch.countDown();
            });
            t.start();
        }
        Thread.sleep(1000);
        latch.await();
    }
}