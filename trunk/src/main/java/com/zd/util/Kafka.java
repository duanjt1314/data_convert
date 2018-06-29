package com.zd.util;

import com.zd.config.SystemConfig;
import com.zd.kafka.JavaKafkaProducer;

public class Kafka {
	private static JavaKafkaProducer kafkaProducer = null;

	static {
		String kafkaUrl = SystemConfig.KafkaUrl;
		kafkaProducer = new JavaKafkaProducer(kafkaUrl);
	}
	

	/**
	 * 发送转换数据到kafka
	 * @param content
	 */
	public static void SendTransKafka(String content) {
		kafkaProducer.send(SystemConfig.TransTopic, content);
	}
}
