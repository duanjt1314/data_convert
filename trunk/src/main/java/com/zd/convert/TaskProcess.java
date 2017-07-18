package com.zd.convert;

import java.util.UUID;

import com.zd.config.ConvertFirm;
import com.zd.config.ConvertTask;
import com.zd.kafka.JavaKafkaConsumerHighAPI;
import com.zd.kafka.KafkaAction;
import com.zd.util.LogHelper;

public class TaskProcess extends Thread {
	/**
	 * 任务对象
	 */
	private ConvertTask TaskInfo;
	/**
	 * kafka的地址
	 */
	private String KafkaUrl;
	/**
	 * 厂商对象
	 */
	private ConvertFirm FirmInfo;

	private JavaKafkaConsumerHighAPI javaKafkaConsumer;

	/**
	 * 初始化任务线程对象
	 * 
	 * @param taskInfo
	 * @param kafkaUrl
	 */
	public TaskProcess(ConvertTask taskInfo, String kafkaUrl, ConvertFirm firmInfo) {
		super();
		TaskInfo = taskInfo;
		KafkaUrl = kafkaUrl;
		FirmInfo = firmInfo;
	}

	@Override
	public void run() {
		javaKafkaConsumer = new JavaKafkaConsumerHighAPI(TaskInfo.Topic, 1, KafkaUrl, TaskInfo.TaskId, new KafkaAction() {

			@Override
			public void RecevieMsg(String msg) {
				try {
					String convertId = UUID.randomUUID().toString();
					LogHelper.getLogger()
							.debug("接收到数据:" + System.lineSeparator()//
									+ "转换唯一编码:" + convertId + System.lineSeparator()//
									+ msg + System.lineSeparator()//
					);
					new FileConvert(TaskInfo, FirmInfo, convertId).DealFile(msg);
				} catch (Exception e) {
					LogHelper.getLogger().error("接收数据处理,未识别的异常", e);
				}
			}
		});
		new Thread(javaKafkaConsumer).start();
		LogHelper.getLogger().info(String.format("KakfaURL:%s,Topic:%s,监听成功", KafkaUrl, TaskInfo.Topic));
	}

	/**
	 * 启动
	 */
	public void Start() {
		start();
		LogHelper.getLogger().info(String.format("厂商编码:%s,任务编码:%s启动成功", FirmInfo.FirmId, TaskInfo.TaskId));
	}

	/**
	 * 停止
	 */
	public void Stop() {
		if (javaKafkaConsumer != null) {
			javaKafkaConsumer.shutdown();
		}
		LogHelper.getLogger().info(String.format("厂商编码:%s,任务编码:%s停止成功", FirmInfo.FirmId, TaskInfo.TaskId));
	}

}
