package com.zd.kafka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import com.zd.config.ConvertFirm;
import com.zd.config.ConvertTask;
import com.zd.config.SystemConfig;
import com.zd.convert.FileConvert;
import com.zd.util.DBAction;
import com.zd.util.Helper;
import com.zd.util.LogHelper;
import com.zd.util.TableTitle;

import cn.zdsoft.common.StringUtil;
import cn.zdsoft.common.model.DataTable;

/**
 * 从Kafka定时获取数据的线程
 * @author 段江涛
 * @date 2018-12-20
 */
public class KafKaProcess {
	private String groupId = "data_convert";
	private ConvertTask convertTask;
	private ConvertFirm firmInfo;
	private String convertId;
	private Consumer<String, String> consumer;
	private boolean isRunning = false;

	public KafKaProcess(ConvertTask convertTask, ConvertFirm firmInfo, String zookeeper) {
		this.convertTask = convertTask;
		this.firmInfo = firmInfo;
		this.convertId = UUID.randomUUID().toString();

		// 初始化Kafka消费者
		Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, zookeeper);// 服务器ip:端口号，集群用逗号分隔
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 5000);
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);// 手动提交
		consumer = new KafkaConsumer<>(props);// 实例化消费者
		consumer.subscribe(Arrays.asList(convertTask.Topic.split(",")));// 订阅消息

		LogHelper.logger.info(String.format("kafka消费者初始化成功，url:%s,topic:%s,groupId:%s", zookeeper, convertTask.Topic, groupId));
	}

	public void start() {
		isRunning = true;
		Runnable runnable = new Runnable() {
			public void run() {
				try {
					fromKafka();
				} catch (Exception e) {
					LogHelper.logger.error("", e);
				}
			}
		};
		new Thread(runnable).start();
	}

	/**
	 * 从Kafka获取数据
	 */
	private void fromKafka() {
		List<String> list = new ArrayList<String>();
		long time = System.currentTimeMillis();
		while (isRunning) {
			try {
				ConsumerRecords<String, String> records = consumer.poll(100);// 拉取消息
				if (records.count() > 0) {
					for (ConsumerRecord<String, String> record : records) {
						list.add(record.value());

						// 判断数据大于5000就写入文件
						if (list.size() == 5000) {
							time = System.currentTimeMillis();
							dealData(list);
							list.clear();
							consumer.commitAsync();
						}
					}
				}
				// 超过了指定的时间，也要写入文件
				if (System.currentTimeMillis() - time > convertTask.ListenSec * 1000 && list.size() > 0) {
					time = System.currentTimeMillis();
					dealData(list);
					list.clear();
					consumer.commitAsync();
				}
			} catch (Exception ex) {
				LogHelper.logger.error("taskId:" + convertTask.TaskId + "转换异常，停60秒", ex);
				wait(60);
			}
		}

	}

	private void wait(int seconds) {
		int i = 0;
		while (i < seconds && isRunning) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			i++;
		}
	}

	/**
	 * 处理数据
	 * @param list
	 */
	private void dealData(List<String> list) {
		String title = TableTitle.getTitle(convertTask.Topic);
		String content = StringUtils.join(list, "\r\n");
		content = title + "\r\n" + content;

		LogHelper.logger.info("任务:" + convertTask.TaskId + ",接收到数据,准备解析(topic:" + convertTask.Topic + "):\r\n" + content);

		DataTable dataTable = StringUtil.String2Map(content);
		new FileConvert(convertTask, firmInfo, convertId).DealFile(dataTable, "");
	}

	public void finish() {
		isRunning = false;
	}
}
