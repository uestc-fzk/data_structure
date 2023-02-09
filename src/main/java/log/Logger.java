package log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 实现组提交的日志打印
 *
 * @author fzk
 * @datetime 2023-02-09 12:02:06
 */
@SuppressWarnings("unused")
public class Logger {
    public static void fatal(String msg) {
        addMsg(LogLevel.FATAL, msg);
    }

    public static void error(String msg) {
        addMsg(LogLevel.ERROR, msg);
    }

    public static void warning(String msg) {
        addMsg(LogLevel.WARNING, msg);
    }

    public static void info(String msg) {
        addMsg(LogLevel.INFO, msg);
    }

    public static void debug(String msg) {
        addMsg(LogLevel.DEBUG, msg);
    }

    public static void fine(String msg) {
        addMsg(LogLevel.FINE, msg);
    }

    private static void addMsg(LogLevel level, String msg) {
        LogRecord logRecord = new LogRecord(level, msg, LocalDateTime.now(), 4);
        try {
            lock.lockInterruptibly();
            try {
                // 写满了，等待, 必须用while，会有很多情况下会唤醒
                while (queueWrite.size() >= 1024) {
                    emptyCond.await();
                }
                queueWrite.add(logRecord);
                flushTread.wakeUp();// 唤醒刷新线程
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static volatile ArrayList<LogRecord> queueWrite = new ArrayList<>(1024);
    private static volatile ArrayList<LogRecord> queueRead = new ArrayList<>(1024);
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition emptyCond = lock.newCondition();
    private static final FlushThread flushTread = new FlushThread();
    private static final FileChannel file;

    static {
        try {
            // 先确保已经创建目录
            Path logPath = Path.of("logs/info.log");
            if (Files.notExists(logPath.getParent()))
                Files.createDirectory(logPath.getParent());
            file = FileChannel.open(logPath, Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        flushTread.setDaemon(true);// 必须设为后台线程
        flushTread.start();
    }

    /**
     * 刷新线程：已实现组提交
     */
    private static class FlushThread extends Thread {
        public volatile boolean isAwake = false;// 刷新线程活跃状态：避免冗余唤醒
        private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 轮转队列: 组提交
        private void turnQueue() {
            if (queueWrite.size() > 0) {
                // 轮转队列
                ArrayList<LogRecord> tmp = queueWrite;
                queueWrite = queueRead;
                queueRead = tmp;
                emptyCond.signalAll();// 唤醒所有等待线程
            }
        }

        public void wakeUp() {
            if (!isAwake) {// 避免冗余唤醒
                LockSupport.unpark(flushTread);
                //System.out.println("唤醒");
            }
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    isAwake = true;
                    // 轮转队列
                    lock.lockInterruptibly();
                    try {
                        turnQueue();
                    } finally {
                        lock.unlock();
                    }

                    if (queueRead.size() > 0) {
                        handleRead();
                    } else {
                        isAwake = false;
                        LockSupport.parkNanos(10_000_000_000L);// 等待10s
                    }
                }
            } catch (InterruptedException | IOException e) {
                System.err.println("flush log thread occurs error: " + e);
            } finally {
                try {
                    file.close();
                } catch (IOException ex) {
//                    throw new RuntimeException(ex);
                }
            }
        }

        public void handleRead() throws IOException {
            for (LogRecord record : queueRead) {
                // level time caller msg
                String content = String.format("%s %s %s %s\n", record.level, format.format(record.time), record.caller, record.msg);
                file.write(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));
            }
            file.force(true);// 落盘
            //System.out.println(queueRead.size());
            queueRead.clear();// 清空队列
        }
    }
}
