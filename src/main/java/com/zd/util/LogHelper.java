package com.zd.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import cn.zdsoft.common.PathUtil;

public class LogHelper {
	private static Logger logger = null;

	/**
	 * 获取日志对象
	 * 
	 * @return
	 */
	public static Logger getLogger() {
		if (logger == null) {
			// 改变系统参数
			System.setProperty("log4j.configurationFile", PathUtil.Combine(Helper.GetAppDir(), "config", "log4j2.xml"));
			// 重新初始化Log4j2的配置上下文
			LoggerContext context = (LoggerContext) LogManager.getContext(false);
			context.reconfigure();
			logger = LogManager.getLogger(context);
		}
		return logger;
	}
}
