package com.fzk.jdbc;

import com.alibaba.druid.pool.DruidDataSourceFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * @author zhike.feng
 * @datetime 2023-07-22 23:09:09
 */
public class MyConnManager {

    // 从配置文件jdbc.properties中加载连接数据库的配置信息
    // 必须有url/driver/user/password这四个配置
    public Properties getConnInfo() {
        Properties p = new Properties();
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("jdbc.properties")) {
            p.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(p);
        return p;
    }

    public Connection getConn1() throws Exception {
        Properties p = getConnInfo();
        // 1.加载驱动
        Driver driver = new com.mysql.cj.jdbc.Driver();
        //// 2.注册驱动
        //DriverManager.registerDriver(driver);
        // 3.建立连接
        return driver.connect(p.getProperty("url"), p);
    }

    public Connection getConn2() throws Exception {
        Properties p = getConnInfo();
        // 1.加载驱动
        Class.forName(p.getProperty("driverClassName"));
        //// 2.注册驱动
        //Driver driver = (Driver) clazz.getConstructor().newInstance();
        //DriverManager.registerDriver(driver);
        // 3.建立连接
        return DriverManager.getConnection(p.getProperty("url"), p.getProperty("username"), p.getProperty("password"));
    }

    public DataSource getDruidDataSource() {
        Properties p = getConnInfo();
        try {
            return DruidDataSourceFactory.createDataSource(p);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
