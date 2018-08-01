package com.zd.convert;

import java.io.IOException;

import com.zd.util.LogHelper;

public class DataConvertService {
	private static String VERSION = "1.3.1";

	/**
	 * 程序启动入口,默认配置文件需在jar包同级
	 * 
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void main(String[] args) {
		try {
			if (args.length == 1) {
				if (args[0].equals("-v")) {
					System.out.println("Version:" + VERSION);
				}
				return;
			}
			StartUp startUp = new StartUp();
			LogHelper.getLogger().info("转换程序启动中...");
			startUp.Start();
			// 添加程序结束监听 ，用于释放系统资源
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					startUp.Stop();// 系统结束
				}
			});

			LogHelper.getLogger().info("转换程序启动成功");
		} catch (Exception ex) {
			LogHelper.getLogger().error("转换程序出现未识别的异常", ex);
		}
	}

}
