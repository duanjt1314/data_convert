package com.zd.kafka;

import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.message.MessageAndMetadata;

/**
 * Kafka消费者数据处理线程
 * 
 * @author Administrator
 *
 */
public class ConsumerKafkaStreamProcesser implements Runnable {

	// Kafka数据流
	private KafkaStream<String, String> stream;
	// 线程ID编号
	private int threadNumber;
	private KafkaAction kafkaAction;

	public ConsumerKafkaStreamProcesser(KafkaStream<String, String> stream, int threadNumber,
			KafkaAction kafkaAction) {
		this.stream = stream;
		this.threadNumber = threadNumber;
		this.kafkaAction = kafkaAction;
	}

	@Override
    public void run() {
        // 1. 获取数据迭代器
        ConsumerIterator<String, String> iter = this.stream.iterator();
        // 2. 迭代输出数据
        while (iter.hasNext()) {
            // 2.1 获取数据值
            MessageAndMetadata value = iter.next();

            // 2.2 输出
            System.out.println(this.threadNumber + ":" + ":" + value.offset() + value.key() + ":" + value.message());
            if(kafkaAction!=null){
            	String message=value.message().toString();
            	kafkaAction.RecevieMsg(message);
            }
        }
        // 3. 表示当前线程执行完成
        System.out.println("Shutdown Thread:" + this.threadNumber);
    }

}
