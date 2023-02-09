import log.Logger;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        for (int i = 0; i < 10000; i++) {
            Logger.info(i+"");
            Logger.warning(i+"warning");
        }
    }
}