package com.zd.convert;

import java.util.ArrayList;
import java.util.List;

import com.zd.config.ConvertFirm;
import com.zd.config.ConvertTask;
import com.zd.config.SystemConfig;
import com.zd.util.LogHelper;

/**
 * 具体逻辑处理程序
 * 
 * @author Administrator
 *
 */
public class StartUp extends Thread {
	private List<TaskProcess> Tasks = new ArrayList<TaskProcess>();

	@Override
	public void run() {
		try {
			// 读取配置,拉取kafka,创建线程一系列操作在这里
			for (ConvertFirm firm : SystemConfig.ConvertFirms) {
				for (ConvertTask task : firm.Tasks) {
					TaskProcess t = new TaskProcess(task, SystemConfig.KafkaUrl, firm);
					t.Start();
					Tasks.add(t);
				}
			}

		} catch (Exception e) {
			LogHelper.getLogger().error("线程执行错误", e);
		}
	}

	public void Start() {
		start();
	}

	public void Stop() {
		for (TaskProcess task : Tasks) {
			if (task != null) {
				task.Stop();
			}
		}
		Tasks.clear();
		LogHelper.getLogger().info("程序停止成功");
	}
}
