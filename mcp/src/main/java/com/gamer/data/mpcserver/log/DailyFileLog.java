package com.gamer.data.mpcserver.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 每日滚动文件日志。
 *
 * <p>策略：</p>
 * <ul>
 *   <li>每条日志同时打印到 stderr，并追加到 {@code logDir/yyyy-MM-dd.log}。</li>
 *   <li>按天切换文件：当日期变化时重新打开 writer。</li>
 *   <li>写文件失败不影响主流程（stderr 已有日志）。</li>
 * </ul>
 *
 * <p>性能优化：</p>
 * <ul>
 *   <li>{@link SimpleDateFormat} 作为字段缓存复用，不在每条日志重新 new。</li>
 *   <li>每条日志不再立即 flush；每隔 {@value #FLUSH_INTERVAL_MS} 毫秒或进程退出时才 flush，
 *       避免高频写日志时的 I/O 开销。</li>
 * </ul>
 *
 * <p>线程安全：内部用同一把锁串行化 writer 的创建/写入/关闭。</p>
 */
public class DailyFileLog implements Log {
    /** 批量 flush 间隔（毫秒）。 */
    private static final long FLUSH_INTERVAL_MS = 2000L;

    private final File logDir;//日志目录（不存在会尝试创建）
    private final Object lock = new Object();//用于串行化 writer 的创建/写入/关闭

    private String currentDay;//当前日期（用于按天切换文件）
    private BufferedWriter writer;//当前 writer（用于写入日志）

    /** 复用的 SimpleDateFormat（仅在 synchronized(lock) 内使用，无线程安全问题）。 */
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    /** 复用的日期格式（仅在 synchronized(lock) 内使用）。 */
    private final SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd");

    /** 上次 flush 的时间戳（毫秒）。 */
    private long lastFlushTime;

    /**
     * @param logDir 日志目录（不存在会尝试创建）
     */
    public DailyFileLog(File logDir) {
        this.logDir = logDir;
    }

    @Override
    public void logMessage(String message) {
        logMessage(message, false);
    }

    @Override
    public void logMessage(String message, boolean redShow) {
        String msg = message == null ? "null" : message;
        String line;
        synchronized (lock) {
            line = timeFmt.format(new Date()) + " " + msg;
        }
        System.err.println(line);
        appendLine(line);
    }

    /**
     * 关闭当前 writer（如果存在）。
     *
     * <p>入口退出时调用；多次调用安全。会先执行最终 flush。</p>
     */
    public void close() {
        synchronized (lock) {
            try {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            } catch (Exception ignored) {
            } finally {
                writer = null;
                currentDay = null;
            }
        }
    }

    private void appendLine(String line) {
        synchronized (lock) {
            try {
                ensureWriter();
                writer.write(line);
                writer.newLine();

                // 批量 flush：距上次 flush 超过阈值才真正执行，减少系统调用频率。
                long now = System.currentTimeMillis();
                if (now - lastFlushTime >= FLUSH_INTERVAL_MS) {
                    writer.flush();
                    lastFlushTime = now;
                }
            } catch (Exception e) {
                // stderr 已打印，文件失败不阻断主流程
                System.err.println(timeFmt.format(new Date()) + " [LOG_WRITE_FAIL] " + e);
            }
        }
    }

    private void ensureWriter() throws Exception {
        if (!logDir.exists()) {
            boolean ok = logDir.mkdirs();
            if (!ok && !logDir.exists()) {
                throw new IllegalStateException("无法创建日志目录: " + logDir.getAbsolutePath());
            }
        }

        String day = dayFmt.format(new Date());
        if (writer != null && day.equals(currentDay)) {
            return;
        }

        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (Exception ignored) {
            }
        }

        File logFile = new File(logDir, day + ".log");
        writer = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8));
        currentDay = day;
        lastFlushTime = System.currentTimeMillis();
    }
}
