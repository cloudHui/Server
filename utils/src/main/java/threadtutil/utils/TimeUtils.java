package threadtutil.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

/**
 * 时间工具。格式化使用线程安全的 DateTimeFormatter，避免 SimpleDateFormat 的同步与重复创建开销。
 */
public final class TimeUtils {
	public static final int SECOND = 1;
	public static final int MINUTE = 2;
	public static final int HOUR = 3;
	public static final int DATE = 4;
	public static final int WEEK = 5;
	public static final int MONTH = 6;
	public static final int YEAR = 7;

	private static final DateTimeFormatter DATE_TIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final ZoneId ZONE = ZoneId.systemDefault();
	private static final Date EPOCH = Date.from(Instant.EPOCH);

	/** 默认线程数建议值：CPU 核数 * 2。 */
	public static int PROCESS_NUMBER = Math.max(2, Runtime.getRuntime().availableProcessors() * 2);

	private TimeUtils() {
	}

	public static Date defaultTime() {
		return EPOCH;
	}

	public static Date now() {
		return new Date();
	}

	public static long time() {
		return System.currentTimeMillis();
	}

	public static long delayTime(long time) {
		return System.currentTimeMillis() + 1000L * time;
	}

	public static Date getDate(long mills) {
		return new Date(mills);
	}

	public static String getDate(Date date) {
		return LocalDateTime.ofInstant(date.toInstant(), ZONE).format(DATE_TIME);
	}

	public static String getStrDate(long mills) {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(mills), ZONE).format(DATE_TIME);
	}

	public static long diffTime(Date d1, Date d2) {
		return (d1.getTime() - d2.getTime()) / 1000L;
	}

	public static Date getDelayDate(Date date, long second) {
		return new Date(date.getTime() + second * 1000L);
	}

	public static Date getDelaySecond(Date date, int delay) {
		return getDelay(date, SECOND, delay);
	}

	/**
	 * @param date  需要延迟的起始时间
	 * @param type  延迟类型 SECOND/MINUTE/HOUR/DATE/WEEK/MONTH/YEAR
	 * @param delay 延迟的数量
	 * @return 延迟后的时间
	 */
	public static Date getDelay(Date date, int type, int delay) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		switch (type) {
			case SECOND:
				calendar.add(Calendar.SECOND, delay);
				break;
			case MINUTE:
				calendar.add(Calendar.MINUTE, delay);
				break;
			case HOUR:
				calendar.add(Calendar.HOUR, delay);
				break;
			case DATE:
				calendar.add(Calendar.DATE, delay);
				break;
			case WEEK:
				calendar.add(Calendar.DATE, delay * 7);
				break;
			case MONTH:
				calendar.add(Calendar.MONTH, delay);
				break;
			case YEAR:
				calendar.add(Calendar.YEAR, delay);
				break;
			default:
				break;
		}
		return calendar.getTime();
	}
}
