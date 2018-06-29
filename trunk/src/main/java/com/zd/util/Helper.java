package com.zd.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.zd.config.SystemConfig;
import com.zd.kafka.JavaKafkaProducer;

import cn.zdsoft.common.DateUtil;
import cn.zdsoft.common.model.DataRow;
import cn.zdsoft.common.model.DataTable;

public class Helper {
	private static int sequence5 = 1;
	private static Lock lock = new ReentrantLock();// 定义锁对象
	
	/**
	 * 获取当前程序所运行jar所在目录
	 * 
	 * @return
	 */
	public static String GetAppDir() {
		return System.getProperty("user.dir");
	}

	/**
	 * 获得当前时间的时间戳，也就是距离1970-01-01的秒数
	 * 
	 * @return
	 */
	public static long GetTimeInt() {
		return new Date().getTime() / 1000;
	}

	/**
	 * 获得当前时间的字符串，如：20170120112005
	 * 
	 * @return
	 */
	public static String GetTimeString() {
		return DateUtil.Format(new Date(), "yyyyMMddHHmmss");
	}

	/**
	 * 1-99999之间的数，自动轮询
	 * 
	 * @return
	 */
	public static String getSequence5() {
		lock.lock();
		try {
			if (sequence5 >= 99999) {
				sequence5 = 1;
			} else {
				sequence5 += 1;
			}
			return String.format("%05d", sequence5);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 将xml读取为DataTable
	 * @param content
	 * @return
	 * @throws DocumentException 
	 */
	public static DataTable Xml2Map(File xmlFile) throws DocumentException {
		SAXReader reader = new SAXReader();
		Document document = reader.read(xmlFile);
		Element root = document.getRootElement();
		DataTable dataTable = new DataTable();
		for (Object obj : root.elements()) {
			DataRow row = new DataRow();
			Element element = (Element) obj;

			for (Object object : element.elements()) {
				Element ele_item = (Element) object;
				row.put(ele_item.getName(), ele_item.getText());
			}
			dataTable.add(row);
		}
		return dataTable;
	}

	/**
	 * 将xml读取为DataTable
	 * @param content
	 * @return
	 * @throws DocumentException 
	 * @throws UnsupportedEncodingException 
	 */
	public static DataTable Xml2Map(String xml) throws DocumentException, UnsupportedEncodingException {
		SAXReader reader = new SAXReader();
		Document document = reader.read(new ByteArrayInputStream(xml.getBytes("utf-8")));
		Element root = document.getRootElement();
		DataTable dataTable = new DataTable();
		for (Object obj : root.elements()) {
			DataRow row = new DataRow();
			Element element = (Element) obj;

			for (Object object : element.elements()) {
				Element ele_item = (Element) object;
				row.put(ele_item.getName(), ele_item.getText());
			}
			dataTable.add(row);
		}
		return dataTable;
	}

}
