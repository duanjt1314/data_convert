package com.zd.convert;

import java.util.UUID;

import com.zd.config.ConvertFirm;
import com.zd.config.ConvertTask;
import com.zd.kafka.JavaKafkaConsumerHighAPI;
import com.zd.kafka.KafkaAction;
import com.zd.util.LogHelper;

public class TaskProcess {
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
	
	public void DealData(String msg){
		String convertId = UUID.randomUUID().toString();
		new FileConvert(TaskInfo, FirmInfo, convertId).DealFile(msg);
	}
}
