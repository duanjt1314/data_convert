package com.zd.kafka;

import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.zd.util.LogHelper;

import cn.zdsoft.common.StringUtil;

/**
 * 生产者
 * @author 段江涛
 * @date 2018-06-28
 */
public class JavaKafkaProducer {
	Producer<String, String> producer = null;

	public JavaKafkaProducer(String zookeeper) {
		if (!StringUtil.IsNullOrEmpty(zookeeper)) {
			Properties props = new Properties();
			props.put("bootstrap.servers", zookeeper);// 服务器ip:端口号，集群用逗号分隔
			props.put("acks", "all");
			props.put("retries", 0);
			props.put("batch.size", 16384);
			props.put("linger.ms", 1);
			props.put("buffer.memory", 33554432);
			props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
			props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
			producer = new KafkaProducer(props);
		}
	}

	public void send(String topic, String content) {
		if (producer != null) {
			producer.send(new ProducerRecord<String, String>(topic, UUID.randomUUID().toString(), content));
			LogHelper.getLogger().debug("send to kafka.topic:" + topic + " |content:" + content);
		}else{
			LogHelper.getLogger().debug("kafka producer is null,can not send.maybe kafkaurl is null");
		}
	}
}
