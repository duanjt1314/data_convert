package com.zd.util;

import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cn.zdsoft.common.DateUtil;

public class Helper {
	private static int sequence5 = 1;
	private static Lock lock = new ReentrantLock();// 定义锁对象

	/**
	 * 获取当前程序所运行jar所在目录
	 * 
	 * @return
	 */
	public static String GetAppDir() {
		return System.getProperty("user.dir");
	}

	/**
	 * 获得当前时间的时间戳，也就是距离1970-01-01的秒数
	 * 
	 * @return
	 */
	public static long GetTimeInt() {
		return new Date().getTime() / 1000;
	}

	/**
	 * 获得当前时间的字符串，如：20170120112005
	 * 
	 * @return
	 */
	public static String GetTimeString() {
		return DateUtil.Format(new Date(), "yyyyMMddHHmmss");
	}

	/**
	 * 1-99999之间的数，自动轮询
	 * 
	 * @return
	 */
	public static String getSequence5() {
		lock.lock();
		try {
			if (sequence5 >= 99999) {
				sequence5 = 1;
			} else {
				sequence5 += 1;
			}
			return String.format("%05d", sequence5);
		} finally {
			lock.unlock();
		}
	}
}
