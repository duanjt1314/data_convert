package com.zd.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;
import org.springframework.util.ResourceUtils;

public class LogHelper {
	public static Logger logger = LogManager.getLogger(LogHelper.class);
	private static Map<String, Logger> hash = new HashMap();

	public static Logger getLogger() {
		return logger;
	}

	/**
	 * 根据任务编码获取日志记录器
	 * @param taskId
	 * @return
	 */
	public static Logger getLogger(String taskId) {
		if (hash.containsKey(taskId))
			return hash.get(taskId);

		synchronized (hash) {
			Properties properties = new Properties();
			String logPath = "";
			try {
				File file = ResourceUtils.getFile("classpath:log4j.properties");
				InputStream inputStream = new FileInputStream(file);
				properties.load(inputStream);
				logPath = properties.get("log.dir").toString();
			} catch (Exception e) {
				e.printStackTrace();
			}

			Logger logger = Logger.getLogger(LogHelper.class);
			RollingFileAppender appender = new RollingFileAppender();
			appender.setFile(Paths.get(logPath, "kafka-" + taskId + ".txt").toString());
			appender.setAppend(true);
			logger.addAppender(appender);
			hash.put(taskId, logger);
			return logger;
		}
	}
}
