package com.zd.convert;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.zd.config.ConvertFirm;
import com.zd.config.ConvertTask;
import com.zd.config.SystemConfig;
import com.zd.kafka.JavaKafkaConsumerHighAPI;
import com.zd.kafka.KafkaAction;
import com.zd.util.LogHelper;

/**
 * 具体逻辑处理程序
 * 
 * @author Administrator
 *
 */
public class StartUp extends Thread {
	private JavaKafkaConsumerHighAPI javaKafkaConsumer;

	@Override
	public void run() {
		try {
			// 循环并获取需要消费的topic集合
			List<String> topics = new ArrayList<String>();
			for (ConvertFirm firm : SystemConfig.ConvertFirms) {
				for (ConvertTask task : firm.Tasks) {
					topics.add(task.Topic);
				}
			}

			javaKafkaConsumer = new JavaKafkaConsumerHighAPI(topics, 1, SystemConfig.KafkaUrl, "zdkafka", new KafkaAction() {

				@Override
				public void RecevieMsg(String msg, String topic) {
					try {
						String convertId = UUID.randomUUID().toString();
						LogHelper.getLogger()
								.debug("接收到数据:" + System.lineSeparator()//
										+ "转换唯一编码:" + convertId + System.lineSeparator()//
										+ msg + System.lineSeparator()//
						);
						
						for (ConvertFirm firm : SystemConfig.ConvertFirms) {
							for (ConvertTask task : firm.Tasks) {
								if(task.Topic.equals(topic)){
									new FileConvert(task, firm, convertId).DealFile(msg);
								}
							}
						}
						
					} catch (Exception e) {
						LogHelper.getLogger().error("接收数据处理,未识别的异常", e);
					}
				}
			});
			new Thread(javaKafkaConsumer).start();
			LogHelper.getLogger().info(String.format("KakfaURL:%s,Topic:%s,监听成功", SystemConfig.KafkaUrl, topics));

		} catch (Exception e) {
			LogHelper.getLogger().error("线程执行错误", e);
		}
	}

	public void Start() {
		start();
	}

	public void Stop() {
		javaKafkaConsumer.shutdown();
		LogHelper.getLogger().info("程序停止成功");
	}
}
