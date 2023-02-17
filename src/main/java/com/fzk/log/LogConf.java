package com.fzk.log;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 日志配置类
 *
 * @author fzk
 * @datetime 2023-02-09 21:35:30
 */
public class LogConf {
    private String logPath;
    private String logLevel;
    private int logQueueSize;// 日志队列大小，建议1024
    private long logFileSize;// 日志文件大小，建议16MB，即16*1024*1024


    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public int getLogQueueSize() {
        return logQueueSize;
    }

    public void setLogQueueSize(int logQueueSize) {
        this.logQueueSize = logQueueSize;
    }

    public long getLogFileSize() {
        return logFileSize;
    }

    public void setLogFileSize(long logFileSize) {
        this.logFileSize = logFileSize;
    }

    @Override
    public String toString() {
        return "LogConf{" +
                "logPath='" + logPath + '\'' +
                ", logLevel='" + logLevel + '\'' +
                ", logQueueSize=" + logQueueSize +
                ", logFileSize=" + logFileSize +
                '}';
    }

    public static LogConf getDefaultLogConf() {
        LogConf conf = new LogConf();
        conf.setLogLevel("info");
        conf.setLogPath("logs/info.log");
        conf.setLogQueueSize(1024);
        conf.setLogFileSize(16 * 1024 * 1024);// 16MB
        return conf;
    }

    // 自动探测配置文件从而解析日志配置
    public static LogConf detectLogConf() throws IOException {
        // 1.优先加载类路劲下 log.properties
        System.out.println("日志配置探测：类路径下log.properties");
        try (InputStream in = LogConf.class.getClassLoader().
                getResourceAsStream("log.properties")) {
            if (in != null) {// 说明不存在或没找到
                System.out.println("日志配置探测成功：读取类路径下的log.properties");
                Properties p = new Properties();
                p.load(in);
                System.out.println("类路径下的配置为：" + p);
                return parseToConf(p);
            }
        }

        // 2.再尝试加载当前工作目录下 conf/log.properties
        System.out.println("日志配置探测：工作路径下conf/log.properties");
        if (Files.exists(Path.of("conf/log.properties"))) {
            try (FileReader fileReader = new FileReader("conf/log.properties")) {
                System.out.println("日志配置探测成功：读取工作目录下conf/log.properties");
                Properties p = new Properties();
                p.load(fileReader);
                System.out.println("conf/log.properties下的配置为：" + p);
                return parseToConf(p);
            }
        }

        // 3.返回默认配置
        LogConf logConf= getDefaultLogConf();
        System.err.println("未探测到日志配置文件: 类路劲下log.properties或当前工作目录下conf/log.properties");
        System.out.println("日志默认配置: "+logConf);
        return logConf;
    }

    private static LogConf parseToConf(Properties p) {
        LogConf logConf = new LogConf();
        logConf.logPath = p.getProperty("logPath");
        logConf.logLevel = p.getProperty("logLevel");
        logConf.logQueueSize = Integer.parseInt(p.getProperty("logQueueSize", "0"));
        logConf.logFileSize = Integer.parseInt(p.getProperty("logFileSize", "0"));
        // 检查
        if (logConf.logPath == null || logConf.logPath.length() == 0) {
            throw new RuntimeException("缺少属性logPath");
        }
        if (logConf.logLevel == null || logConf.logLevel.length() == 0) {
            throw new RuntimeException("缺少属性logLevel");
        }
        if (logConf.logQueueSize < 128) {
            throw new RuntimeException("缺少属性logQueueSize或值小于128");
        }
        if (logConf.logFileSize < (1 << 20)) {
            throw new RuntimeException("缺少属性logFileSize或值小于1048576(1MB)");
        }
        return logConf;
    }
}
