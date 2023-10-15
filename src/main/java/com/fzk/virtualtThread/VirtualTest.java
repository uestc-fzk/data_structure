package com.fzk.virtualtThread;

import com.alibaba.druid.pool.DruidDataSource;
import com.fzk.jdbc.MyConnManager;
import com.fzk.jdbc.T2;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/**
 * 测试虚拟线程
 *
 * @author zhike.feng
 * @datetime 2023-10-14 22:47:18
 */
public class VirtualTest {
    @Test
    public void createVirtualThread() {
        Thread.ofVirtual().name("virtual-1").start(() -> {
            System.out.println("i am " + Thread.currentThread());
        });
    }

    public void testSleep() {
        // 虚拟线程只运行大概大概1s
        // start: 2023-10-14T23:28:59.945284600, end: 2023-10-14T23:29:01.149835600
        executeSleepTasks(Executors.newVirtualThreadPerTaskExecutor());

        // 200个线程的线程池大概要运行50s
        // start: 2023-10-14T23:29:01.153834800, end: 2023-10-14T23:29:51.634957600
        executeSleepTasks(Executors.newFixedThreadPool(200));
    }

    private void executeSleepTasks(ExecutorService executor) {
        LocalDateTime start = LocalDateTime.now();
        IntStream.range(0, 10_000).forEach(i -> {
            executor.submit(() -> {
                Thread.sleep(1000);// 休眠1s
                return i;
            });
        });
        executor.close();// 执行结束并关闭
        LocalDateTime end = LocalDateTime.now();
        System.out.printf("start: %s, end: %s\n", start, end);
    }

    public void testIO() {
        executeIOTasks(Executors.newVirtualThreadPerTaskExecutor(), (DruidDataSource) new MyConnManager().getDruidDataSource());

        executeIOTasks(Executors.newFixedThreadPool(20), (DruidDataSource) new MyConnManager().getDruidDataSource());
    }

    private void executeIOTasks(ExecutorService executor, DruidDataSource dataSource) {
        dataSource.setMinIdle(100);
        dataSource.setMaxActive(100);
        LocalDateTime start = LocalDateTime.now();
        for (int i = 0; i < 10_000; i++) {
            executor.submit(() -> {
                try (Connection conn = dataSource.getConnection()) {
                    // 解析单条数据
                    BeanHandler<T2> t2Handler = new BeanHandler<>(T2.class);
                    T2 t2 = new QueryRunner().query(conn, "SELECT * FROM t2 WHERE id=?", t2Handler, 2);
//                    System.out.println(t2);

                    // 解析多条数据
                    BeanListHandler<T2> t2sHandler = new BeanListHandler<>(T2.class);
                    List<List<T2>> t2s = new QueryRunner().execute(conn, "SELECT * FROM t2", t2sHandler);
//                    System.out.println(t2s);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        executor.close();
        LocalDateTime end = LocalDateTime.now();
        dataSource.close();
        System.out.printf("start: %s, end: %s\n", start, end);
    }
}
