package com.zd.convert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.zd.config.ConvertFirm;
import com.zd.config.ConvertTask;
import com.zd.config.SystemConfig;
import com.zd.kafka.DBProcess;
import com.zd.kafka.FileScan;
import com.zd.kafka.JavaKafkaConsumerHighAPI;
import com.zd.kafka.KafkaAction;
import com.zd.util.LogHelper;

import cn.zdsoft.common.StringUtil;

/**
 * 具体逻辑处理程序
 * 
 * @author Administrator
 *
 */
public class StartUp extends Thread {
	private JavaKafkaConsumerHighAPI javaKafkaConsumer;
	private FileScan fileScan = new FileScan();
	private List<DBProcess> dBPros = new ArrayList<DBProcess>();

	@Override
	public void run() {
		KafkaListen();
		new Thread(fileScan).start();

		// 数据库
		for (ConvertFirm firm : SystemConfig.ConvertFirms) {
			for (ConvertTask task : firm.Tasks) {
				if (task.DbAble) {
					DBProcess dbProcess = new DBProcess(task, firm);
					dbProcess.start();
					dBPros.add(dbProcess);
				}
			}
		}
	}

	/**
	 * Kafka消息队列监听
	 */
	private void KafkaListen() {
		try {

			if (!StringUtil.IsNullOrEmpty(SystemConfig.KafkaUrl)) {

				// 循环并获取需要消费的topic集合
				List<String> topics = new ArrayList<String>();
				for (ConvertFirm firm : SystemConfig.ConvertFirms) {
					for (ConvertTask task : firm.Tasks) {
						if (!StringUtil.IsNullOrEmpty(task.Topic))
							topics.add(task.Topic);
					}
				}

				if (topics.size() > 0) {

					javaKafkaConsumer = new JavaKafkaConsumerHighAPI(topics, 1, SystemConfig.KafkaUrl, "data_convert", new KafkaAction() {

						@Override
						public void RecevieMsg(String msg, String topic) {
							try {
								String convertId = UUID.randomUUID().toString();
								LogHelper.getLogger()
										.debug("接收到数据(TOPIC:" + topic + "):"//
												+ System.lineSeparator()//
												+ "转换唯一编码:" + convertId + System.lineSeparator()//
												+ msg + System.lineSeparator()//
								);

								for (ConvertFirm firm : SystemConfig.ConvertFirms) {
									for (ConvertTask task : firm.Tasks) {
										if (task.Topic.equals(topic)) {
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
				} else {
					LogHelper.getLogger().warn("kafka url is ok,but topics is null,so not listen");
				}
			} else {
				LogHelper.getLogger().warn("kafka的url地址配置为空，不进行kafka的监听。");
			}

		} catch (Exception e) {
			LogHelper.getLogger().error("kafka监听异常", e);
		}
	}

	public void Start() {
		start();
	}

	public void Stop() {
		javaKafkaConsumer.shutdown();
		fileScan.stop();
		for (DBProcess dbProcess : dBPros) {
			dbProcess.finish();
		}
		LogHelper.getLogger().info("程序停止成功");
	}
}
