package com.zd.config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.*;
import org.dom4j.io.SAXReader;

import com.zd.util.Helper;
import com.zd.util.LogHelper;

import cn.zdsoft.common.*;

/**
 * 系统配置类
 * 
 * @author Administrator
 *
 */
public class SystemConfig {
	/**
	 * 转换厂商集合
	 */
	public static List<ConvertFirm> ConvertFirms = new ArrayList<ConvertFirm>();
	/**
	 * kafka服务器地址
	 */
	public static String KafkaUrl = "";
	/**
	 * 配置文件路径
	 */
	private static String configPath = PathUtil.Combine(Helper.GetAppDir(), "config", "config.xml");
	/**
	 * 文件输出目录
	 */
	public static String OutputDir = Helper.GetAppDir() + File.separator + "output";
	/**
	 * 错误文件存放目录
	 */
	public static String ErrorDir = Helper.GetAppDir() + File.separator + "error";
	/**
	 * 临时文件目录
	 */
	public static String TempDir = Helper.GetAppDir() + File.separator + "temp";
	/**
	 * 转换日志目录
	 */
	public static String ConvertLogDir = Helper.GetAppDir() + File.separator + "convertLog";

	/**
	 * 静态构造函数,加载xml配置
	 */
	static {
		SAXReader reader = new SAXReader();
		try {
			Document document = reader.read(new File(configPath));
			Element root = document.getRootElement();
			analysisKafka(root.element("kafka"));
			if (root.element("firms") != null) {
				for (Object obj : root.element("firms").elements("firm")) {
					Element ele = (Element) obj;
					ConvertFirms.add(analysisFirm(ele));
				}
			}
			if (root.element("appSetting") != null) {
				analysisAppSetting(root.element("appSetting"));
			}

			// 打印日志,显示读取结果
			StringBuilder sb = new StringBuilder();
			sb.append("配置文件" + configPath + "解析完成,解析结果是:" + System.lineSeparator());
			sb.append("任务结果是:" + StringUtil.GetJsonString(ConvertFirms) + System.lineSeparator());
			sb.append("kfaka:" + KafkaUrl);
			LogHelper.getLogger().info(sb.toString());
		} catch (Exception e) {
			LogHelper.getLogger().error("加载xml配置:" + configPath + " 失败", e);
		}
	}

	/**
	 * 解析系统配置
	 * 
	 * @param element
	 *            将传入xml节点 config/appSetting
	 */
	private static void analysisAppSetting(Element element) {
		if (element.element("outputDir") != null) {
			OutputDir = element.element("outputDir").getText();
		}
		if (element.element("errorDir") != null) {
			ErrorDir = element.element("errorDir").getText();
		}
		if (element.element("tempDir") != null) {
			TempDir = element.element("tempDir").getText();
		}
		if (element.element("convertLogDir") != null) {
			ConvertLogDir = element.element("convertLogDir").getText();
		}
	}

	/**
	 * 解析kafka消息队列配置
	 * 
	 * @param element
	 *            将传入xml节点 config/kafka
	 * @throws Exception
	 */
	private static void analysisKafka(Element element) throws Exception {
		// 检查kafka配置
		if (element == null) {
			throw new Exception("节点config/kafka不存在,停止解析");
		}
		if (element.element("url") == null) {
			throw new Exception("节点config/kafka/url不存在,停止解析");
		}
		// 读取kafka配置
		KafkaUrl = element.element("url").getText();
	}

	/**
	 * 解析厂商配置
	 * 
	 * @param element
	 *            将传入节点 config/firms/firm
	 * @return
	 * @throws Exception
	 */
	private static ConvertFirm analysisFirm(Element element) throws Exception {
		ConvertFirm firm = new ConvertFirm();
		if (element.attribute("id") == null) {
			throw new Exception("节点config/firms/firm的属性id不存在,停止解析");
		}
		// 厂商编码
		firm.FirmId = element.attribute("id").getText();
		// 任务
		firm.Tasks = new ArrayList<ConvertTask>();
		List tasks = element.elements("task");
		for (Object obj : tasks) {
			firm.Tasks.add(analysisTask((Element) obj));
		}
		// 字典配置
		String dicUrl = element.attribute("convertDic").getText();
		if (!StringUtil.IsNullOrEmpty(dicUrl)) {
			dicUrl = PathUtil.Combine(Helper.GetAppDir(), dicUrl);
			firm.ConvertDictions = analysisConvertDiction(dicUrl);
		}
		return firm;
	}

	/**
	 * 解析任务
	 * 
	 * @param element
	 *            将传入节点config/firms/firm/task
	 * @return 一个任务对象
	 * @throws Exception
	 */
	private static ConvertTask analysisTask(Element element) throws Exception {
		ConvertTask task = new ConvertTask();
		// id属性
		if (element.attribute("id") == null) {
			throw new Exception("节点config/firms/firm/task的属性id不存在,停止解析");
		}
		task.TaskId = element.attribute("id").getText();
		// topic属性
		if (element.attribute("topic") == null) {
			throw new Exception("节点config/firms/firm/task的属性topic不存在,停止解析");
		}
		task.Topic = element.attribute("topic").getText();
		// indexPath
		if (element.element("indexPath") != null) {
			task.IndexPath = element.element("indexPath").getText();
		}
		// indexName
		if (element.element("indexName") != null) {
			task.IndexName = element.element("indexName").getText();
		}
		// dataPath
		if (element.element("dataPath") == null) {
			throw new Exception("节点config/firms/firm/dataPath不存在,停止解析");
		} else {
			task.DataPath = PathUtil.Combine(Helper.GetAppDir(), element.element("dataPath").getText());
		}
		// dataName
		if (element.element("dataName") == null) {
			throw new Exception("节点config/firms/firm/dataName不存在,停止解析");
		} else {
			task.DataName = element.element("dataName").getText();
		}
		// dataType
		if (element.element("dataType") == null) {
			throw new Exception("节点config/firms/firm/dataType不存在,停止解析");
		} else {
			task.DataType = element.element("dataType").getText();
		}
		// fillter
		if (element.element("filter") != null) {
			task.Filter = new HashMap<String, String>();
			for (Object obj : element.element("filter").elements("item")) {
				Element item = (Element) obj;
				task.Filter.put(item.attribute("key").getText(), item.attribute("value").getText());
			}
		}
		// regionReport
		if (element.element("regionReport") != null) {
			task.RegionReport = element.element("regionReport").getText() != "0";
			task.DefaultRegion = element.element("regionReport").attribute("default").getText();
		}
		// zipName
		if (element.element("zipName") != null) {
			task.ZipName = element.element("zipName").getText();
		}
		// 解析转换列的集合
		File dataFile = new File(task.DataPath);
		if (!dataFile.exists()) {
			throw new Exception("任务编码:" + task.TaskId + ",路径:" + task.DataPath + "不存在");
		}
		task.ConvertColumns = analysisConvertColumn(task.DataPath);

		return task;

	}

	/**
	 * 根据路径获取转换列的集合
	 * 
	 * @param url
	 *            转换列的配置文件
	 * @return 转换列的集合
	 * @throws Exception
	 */
	private static List<ConvertColumn> analysisConvertColumn(String url) throws Exception {
		LogHelper.getLogger().info("准备解析转换列文件:" + url);
		List<ConvertColumn> list = new ArrayList<ConvertColumn>();
		SAXReader reader = new SAXReader();
		try {
			Document document = reader.read(new File(url));
			Element root = document.getRootElement();
			if (root.element("columns") != null) {
				for (Object obj : root.element("columns").elements("column")) {
					Element ele = (Element) obj;
					ConvertColumn column = new ConvertColumn();
					// key
					if (ele.attribute("key") != null)
						column.Key = ele.attribute("key").getText();
					else
						LogHelper.getLogger().error("节点:" + ele.getPath() + "缺少key属性");
					// chn
					if (ele.attribute("chn") != null)
						column.Chn = ele.attribute("chn").getText();
					else
						LogHelper.getLogger().error("节点:" + ele.getPath() + "缺少chn属性");
					// fromfield
					if (ele.attribute("fromfield") != null)
						column.Fromfield = ele.attribute("fromfield").getText();
					else
						LogHelper.getLogger().error("节点:" + ele.getPath() + "缺少fromfield属性");

					// tofield
					if (ele.attribute("tofield") != null)
						column.Tofield = ele.attribute("tofield").getText();
					else
						LogHelper.getLogger().error("节点:" + ele.getPath() + "缺少tofield属性");

					// defaultValue
					if (ele.attribute("defaultValue") != null)
						column.DefaultValue = ele.attribute("defaultValue").getText();
					else
						LogHelper.getLogger().warn("节点:" + ele.getPath() + "缺少defaultValue属性");

					// formate
					if (ele.attribute("formate") != null)
						column.Formate = ele.attribute("formate").getText();
					else
						LogHelper.getLogger().warn("节点:" + ele.getPath() + "缺少formate属性");

					list.add(column);
				}
			} else {
				LogHelper.getLogger().error("文件:" + url + "中缺少节点columns");
			}

			return list;

		} catch (Exception e) {
			LogHelper.getLogger().error("加载xml配置:" + url + " 失败", e);
			throw e;
		}
	}

	/**
	 * 解析字典转换配置文件
	 * 
	 * @param url
	 * @return
	 * @throws Exception
	 */
	private static Map<String, List<ConvertDiction>> analysisConvertDiction(String url) throws Exception {
		LogHelper.getLogger().info("准备解析字典转换文件:" + url);
		File file = new File(url);
		if (!(file.isFile() && file.exists())) {
			throw new RuntimeException("文件:[" + url + "]不存在");
		}

		Map<String, List<ConvertDiction>> map = new HashMap<String, List<ConvertDiction>>();
		SAXReader reader = new SAXReader();
		try {
			Document document = reader.read(file);
			Element root = document.getRootElement();

			for (Object convertObj : root.elements("convert")) {
				List<ConvertDiction> list = new ArrayList<ConvertDiction>();
				Element eleConvert = (Element) convertObj;
				for (Object itemObj : eleConvert.elements("item")) {
					Element eleItem = (Element) itemObj;
					ConvertDiction index = new ConvertDiction();
					index.Val = eleItem.attribute("val").getText();
					index.Export = eleItem.attribute("export").getText();
					index.Name = eleItem.attribute("name").getText();
					list.add(index);
				}
				String key = eleConvert.attribute("key").getText();
				map.put(key, list);
			}

			return map;

		} catch (Exception e) {
			LogHelper.getLogger().error("加载xml配置:" + url + " 失败", e);
			throw e;
		}
	}
}
