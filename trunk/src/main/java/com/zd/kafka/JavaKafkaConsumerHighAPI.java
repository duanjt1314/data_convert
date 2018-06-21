package com.zd.kafka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import com.zd.util.LogHelper;
import com.zd.util.TableTitle;

/**
 * 自定义简单Kafka消费者， 使用高级API
 * 
 * @author Administrator
 *
 */
public class JavaKafkaConsumerHighAPI implements Runnable {
	/**
	 * kafka消费者对象
	 */
	private static Consumer<String, String> consumer;

	/**
	 * 线程池
	 */
	private ExecutorService executorPool;

	private KafkaAction kafkaAction;

	/**
	 * 构造函数
	 *
	 * @param topic
	 *            Kafka消息Topic主题
	 * @param numThreads
	 *            处理数据的线程数/可以理解为Topic的分区数
	 * @param zookeeper
	 *            Kafka的Zookeeper连接字符串
	 * @param groupId
	 *            该消费者所属group ID的值
	 * @param kafkaAction
	 *            接收到消息后触发的方法
	 */
	public JavaKafkaConsumerHighAPI(List<String> topics, int numThreads, String zookeeper, String groupId, KafkaAction kafkaAction) {
		// 1. 创建Kafka连接器
		Properties props = new Properties();
		props.put("bootstrap.servers", zookeeper);// 服务器ip:端口号，集群用逗号分隔
		props.put("group.id", groupId);
		props.put("enable.auto.commit", "true");
		props.put("auto.commit.interval.ms", "1000");
		props.put("session.timeout.ms", "30000");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		consumer = new KafkaConsumer<>(props);
		consumer.subscribe(topics);

		this.kafkaAction = kafkaAction;
	}

	@Override
	public void run() {
		while (true) {
			ConsumerRecords<String, String> records = consumer.poll(100);
			if (records.count() > 0) {
				Map<String, String> map = new HashMap<String, String>();
				for (ConsumerRecord<String, String> record : records) {
					String str = record.value() + "\r\n";
					if (map.containsKey(record.topic())) {
						str = map.get(record.topic()) + str + "\r\n";
					}
					map.put(record.topic(), str);
				}

				String ts = "";
				for (String key : map.keySet()) {
					ts += key + "|";
				}
				LogHelper.getLogger().debug("接收到数据,数据总条数：" + records.count() + ",topic:" + ts);

				if (kafkaAction != null) {
					for (String key : map.keySet()) {
						String content = map.get(key);
						String title = TableTitle.getTitle(key);
						if (!title.equals("")) {
							content = title + "\r\n" + content;
						}
						kafkaAction.RecevieMsg(content, key);
					}

				}

			}
		}
	}

	public void shutdown() {
		// 1. 关闭和Kafka的连接，这样会导致stream.hashNext返回false
		if (this.consumer != null) {
			this.consumer.close();
		}

		// 2. 关闭线程池，会等待线程的执行完成
		if (this.executorPool != null) {
			// 2.1 关闭线程池
			this.executorPool.shutdown();

			// 2.2. 等待关闭完成, 等待五秒
			try {
				if (!this.executorPool.awaitTermination(5, TimeUnit.SECONDS)) {
					System.out.println("Timed out waiting for consumer threads to shut down, exiting uncleanly!!");
				}
			} catch (InterruptedException e) {
				System.out.println("Interrupted during shutdown, exiting uncleanly!!");
			}
		}

	}

}
