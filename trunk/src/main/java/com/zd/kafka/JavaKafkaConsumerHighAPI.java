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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import com.zd.util.LogHelper;
import com.zd.util.TableTitle;

import cn.zdsoft.common.StringUtil;

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
	private boolean isRunning;

	/**
	 * 存放topic和数据的集合，超过5000条就处理
	 */
	private Map<String, ArrayList<String>> dicData = new HashMap<String, ArrayList<String>>();

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
		isRunning = true;
		// 开辟子线程定时去执行写入数据
		Thread t = new Thread() {
			public void run() {
				int i = 0;
				while (isRunning) {
					try {
						if (i > 30) {
							DealData();
							i = 0;
						}
						Thread.sleep(1000);
						i++;
					} catch (Exception e) {
					}
				}
			}
		};
		t.start();

		while (true) {
			ConsumerRecords<String, String> records = consumer.poll(100);
			if (records.count() > 0) {
				LogHelper.getLogger().debug("接收到kafka数据，total:" + records.count());
				for (ConsumerRecord<String, String> record : records) {
					if (!StringUtil.IsNullOrEmpty(record.value())) {
						synchronized (dicData) {
							ArrayList<String> ls = null;
							if (dicData.containsKey(record.topic())) {
								ls = dicData.get(record.topic());
							} else {
								ls = new ArrayList<String>();
							}
							ls.add(record.value());
							dicData.put(record.topic(), ls);
						}
					} else {
						LogHelper.getLogger().warn("从消息队列拉取的消息为空，topic:" + record.topic());
					}

				}

			}
		}
	}

	private void DealData() {
		int pageCount = 5000;
		synchronized (dicData) {
			if (kafkaAction != null) {
				for (String topic : dicData.keySet()) {
					String title = TableTitle.getTitle(topic);
					if (!title.equals("")) {
						if (dicData.get(topic).size() <= pageCount) {
							String content = StringUtils.join(dicData.get(topic), "\r\n");
							content = title + "\r\n" + content;
							kafkaAction.RecevieMsg(content, topic);
						} else {
							// 大于5000条，分页							
							int pageSize = dicData.get(topic).size() / pageCount;
							if (dicData.get(topic).size() % pageCount > 0) {
								pageSize += 1;
							}
							for (int i = 0; i < pageSize; i++) {
								List<String> arr = dicData.get(topic).stream().skip(i * pageCount).limit(pageCount).collect(Collectors.toList());
								String content = StringUtils.join(arr, "\r\n");
								content = title + "\r\n" + content;
								kafkaAction.RecevieMsg(content, topic);
							}
						}
					} else {
						LogHelper.getLogger().error("topic:" + topic + " 在数据库表config_datasource中未找到对应的数据");
					}

					dicData.remove(topic);
				}
			}
		}
	}

	public void shutdown() {
		isRunning = false;
		DealData();

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
