package com.zd.util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class LogHelper {
	public static Logger logger = LogManager.getLogger(LogHelper.class);
	
	public static Logger getLogger(){
		return logger;
	}
}
