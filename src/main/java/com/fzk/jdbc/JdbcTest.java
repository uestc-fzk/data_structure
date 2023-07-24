package com.fzk.jdbc;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * @author zhike.feng
 * @datetime 2023-07-24 22:48
 */
public class JdbcTest {
    @org.junit.jupiter.api.Test
    public void testDataSource() throws SQLException {
        MyConnManager manager = new MyConnManager();
        DruidDataSource dataSource = (DruidDataSource) manager.getDruidDataSource();
        try (Connection conn = dataSource.getConnection()) {
            Operate.select(conn);
        }

    }

    @Test
    public void testDBUtils() throws SQLException {
        QueryRunner runner = new QueryRunner();
        DataSource dataSource = new MyConnManager().getDruidDataSource();
        try (Connection conn = dataSource.getConnection()) {
            // 解析单条数据
            BeanHandler<T2> t2Handler = new BeanHandler<>(T2.class);
            T2 t2 = runner.query(conn, "SELECT * FROM t2 WHERE id=?", t2Handler, 2);
            System.out.println(t2);

            // 解析多条数据
            BeanListHandler<T2> t2sHandler = new BeanListHandler<>(T2.class);
            List<List<T2>> t2s = runner.execute(conn, "SELECT * FROM t2", t2sHandler);
            System.out.println(t2s);
        }
    }
}
