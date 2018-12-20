package com.zd.config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dom4j.*;
import org.dom4j.io.SAXReader;

import com.zd.config.model.FilterType;
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
	 * 文件来源目录
	 */
	public static String SourceDir = "";

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
	 * 数据库访问配置
	 */
	public static DataBase DataBase = null;
	public static ElasticSearch Elastic = null;
	public static String TransTopic = "bocai-logs-translog";

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
			if (root.element("db") != null) {// 数据库配置
				analysisDB(root.element("db"));
			}
			if (root.element("es") != null) {
				analysisES(root.element("es"));
			}
			if (root.elements("import").size() > 0) {
				for (Object obj : root.elements("import")) {
					Element ele = (Element) obj;
					String src = ele.attribute("src").getText();
					String path = PathUtil.Combine(Helper.GetAppDir(), src);
					analysisImport(path);
				}
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
		if (element.element("sourceDir") != null) {
			SourceDir = element.element("sourceDir").getText();
		}
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
	 * 解析数据配置
	 * @param element	将传入xml节点 config/db
	 */
	private static void analysisDB(Element element) {
		DataBase = new DataBase();
		DataBase.setDriver(element.element("driver").getText());
		DataBase.setUrl(element.element("url").getText());
		DataBase.setUserName(element.element("userName").getText());
		DataBase.setPassword(element.element("password").getText());
	}

	/**
	 * 解析数据配置
	 * @param element	将传入xml节点 config/es
	 */
	private static void analysisES(Element element) {
		Elastic = new ElasticSearch();
		// 解析帐号密码
		Elastic.setUser(element.element("user").getText());
		Elastic.setPassword(element.element("password").getText());
		// 解析host主机
		if (element.element("hosts") != null) {
			List eleHosts = element.element("hosts").elements("host");
			if (eleHosts.size() == 0) {
				LogHelper.logger.error("ES解析，节点config/es/hosts下未找到host节点");
			} else {
				List<ElasticSearch.Host> hs = new ArrayList<ElasticSearch.Host>();
				for (Object objHost : eleHosts) {
					Element eleHost = (Element) objHost;
					ElasticSearch.Host host = Elastic.newHost();
					host.setIp(eleHost.attribute("ip").getText());
					host.setPort(XmlUtil.GetXmlAttr(eleHost, "port", 9300));
					hs.add(host);
				}
				Elastic.setHosts(hs);
			}
		} else {
			LogHelper.logger.error("ES解析，未找到节点config/es/hosts");
		}
		// 解析设置
		if (element.element("settings") != null) {
			List eleSettings = element.element("settings").elements("setting");
			if (eleSettings.size() == 0) {
				LogHelper.logger.warn("ES解析，节点config/es/settings下未找到setting节点");
			} else {
				List<ElasticSearch.Setting> settings = new ArrayList<ElasticSearch.Setting>();
				for (Object obj : eleSettings) {
					Element eleSetting = (Element) obj;
					ElasticSearch.Setting setting = Elastic.newSetting();
					setting.setKey(eleSetting.attribute("key").getText());
					setting.setVal(eleSetting.attribute("val").getText());
					settings.add(setting);
				}
				Elastic.setSettings(settings);
			}
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
			LogHelper.getLogger().warn("没有配置config/kafka节点，将不会拉取kafka消息");
			return;
		}
		if (element.element("url") == null) {
			LogHelper.getLogger().warn("没有配置config/kafka/url节点，将不会拉取kafka消息");
			return;
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
			firm.Tasks.add(analysisTask((Element) obj, element));
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
	private static ConvertTask analysisTask(Element element, Element firmElement) throws Exception {
		ConvertTask task = new ConvertTask();
		// id属性
		task.TaskId = XmlUtil.GetXmlAttr(element, "id").getText();
		// topic属性
		task.Topic = XmlUtil.GetXmlAttr(element, "topic", "");
		// fileType属性
		task.FileType = XmlUtil.GetXmlAttr(element, "fileType", "");
		if (!StringUtil.IsNullOrEmpty(task.FileType)) {
			if (!task.FileType.startsWith(".")) {
				task.FileType = "." + task.FileType;
			}
		}

		// 需要解析的文件类型
		if (element.element("searchPattern") != null) {
			List spList = element.element("searchPattern").elements("item");
			for (Object object : spList) {
				task.SearchPatterns.add(((Element) object).getText());
			}
		}
		// indexPath
		if (element.element("indexPath") != null) {
			task.IndexPath = element.element("indexPath").getText();
		} else {
			// 任务没有配置就从厂商节点下获取
			if (firmElement.element("indexPath") != null) {
				task.IndexPath = firmElement.element("indexPath").getText();
			}
		}
		// indexName
		if (element.element("indexName") != null) {
			task.IndexName = element.element("indexName").getText();
		} else {
			// 任务没有配置就从厂商节点下获取
			if (firmElement.element("indexName") != null) {
				task.IndexName = firmElement.element("indexName").getText();
			}
		}
		// dataPath
		if (element.element("dataPath") == null) {
			throw new Exception("节点config/firms/firm/dataPath不存在,停止解析");
		} else {
			task.DataPath = PathUtil.Combine(Helper.GetAppDir(), element.element("dataPath").getText());
		}
		// dataName
		if (element.element("dataName") == null) {
			// 任务没有配置就从厂商节点下获取
			task.DataName = XmlUtil.GetXmlElement(firmElement, "dataName").getText();
		} else {
			task.DataName = element.element("dataName").getText();
		}
		// dataType
		if (element.element("dataType") == null) {
			// 任务没有配置就从厂商节点下获取
			task.DataType = XmlUtil.GetXmlElement(firmElement, "dataType").getText();
		} else {
			task.DataType = element.element("dataType").getText();
		}
		// fillter
		if (element.element("filter") != null) {
			task.Filter = new HashMap<String, List<ConvertFilter>>();
			for (Object obj : element.element("filter").elements("item")) {
				Element item = (Element) obj;
				String key = XmlUtil.GetXmlAttr(item, "key").getText().toUpperCase();
				String type = XmlUtil.GetXmlAttr(item, "type", "VALUE").toUpperCase();
				String value = XmlUtil.GetXmlAttr(item, "value", "");

				ConvertFilter filter = new ConvertFilter(key, value, FilterType.valueOf(type));
				if (task.Filter.containsKey(key)) {
					task.Filter.get(key).add(filter);
				} else {
					List<ConvertFilter> fs = new ArrayList<ConvertFilter>();
					fs.add(filter);
					task.Filter.put(key, fs);
				}
			}
		}
		// regionReport
		if (element.element("regionReport") != null) {
			task.RegionReport = element.element("regionReport").getText() != "0";
			task.DefaultRegion = element.element("regionReport").attribute("default").getText();
		} else if (firmElement.element("regionReport") != null) {
			task.RegionReport = firmElement.element("regionReport").getText() != "0";
			task.DefaultRegion = firmElement.element("regionReport").attribute("default").getText();
		}
		// zipName
		if (element.element("zipName") != null) {
			task.ZipName = element.element("zipName").getText();
		} else {
			if (firmElement.element("zipName") != null) {
				task.ZipName = firmElement.element("zipName").getText();
			}
		}
		// siteIdName(数据质量)
		if (element.element("siteIdName") != null) {
			task.SiteIdName = XmlUtil.GetXmlElement(element, "siteIdName", "").toUpperCase();
		} else {
			task.SiteIdName = XmlUtil.GetXmlElement(firmElement, "siteIdName", "").toUpperCase();
		}
		// deviceIdName(数据质量)
		if (element.element("deviceIdName") != null) {
			task.DeviceIdName = XmlUtil.GetXmlElement(element, "deviceIdName", "").toUpperCase();
		} else {
			task.DeviceIdName = XmlUtil.GetXmlElement(firmElement, "deviceIdName", "").toUpperCase();
		}
		// sourceSiteIdName(数据质量)
		if (element.element("sourceSiteIdName") != null) {
			task.SourceSiteIdName = XmlUtil.GetXmlElement(element, "sourceSiteIdName", "").toUpperCase();
		} else {
			task.SourceSiteIdName = XmlUtil.GetXmlElement(firmElement, "sourceSiteIdName", "").toUpperCase();
		}
		// hasCompress
		if (element.element("hasCompress") != null) {
			task.HasCompress = XmlUtil.GetXmlElement(element, "hasCompress", true);
		} else {
			task.HasCompress = XmlUtil.GetXmlElement(firmElement, "hasCompress", true);
		}
		// hasIndex
		if (element.element("hasIndex") != null) {
			task.HasIndex = XmlUtil.GetXmlElement(element, "hasIndex", true);
		} else {
			task.HasIndex = XmlUtil.GetXmlElement(firmElement, "hasIndex", true);
		}		
		task.DbAble = XmlUtil.GetXmlAttr(element, "dbAble", false);
		task.EsAble = XmlUtil.GetXmlAttr(element, "esAble", false);

		// 解析转换列的集合
		File dataFile = new File(task.DataPath);
		if (!dataFile.exists()) {
			throw new Exception("任务编码:" + task.TaskId + ",路径:" + task.DataPath + "不存在");
		}
		task.ConvertColumns = analysisConvertColumn(task.DataPath);

		if (task.ConvertColumns.size() == 0) {
			LogHelper.getLogger().error("任务:" + task.TaskId + ",转换列为空，也许是list.xml中的column没有配置");
		}

		// 解析数据库转换相关配置（主要用于基础数据的转换）
		if (task.DbAble) {
			Element sqlEle = element.element("sql");
			if (sqlEle == null) {
				LogHelper.getLogger().error("任务:" + task.TaskId + "配置的DbAble为true，但是却没有配置sql节点");
				task.DbAble = false;
			} else {
				task.ConvertSql = new ConvertSql();
				task.ConvertSql.Sql = sqlEle.getText();
				task.ConvertSql.KeyName = sqlEle.attribute("keyName").getText();
				task.ConvertSql.Start = XmlUtil.GetXmlAttr(sqlEle, "start", 0);
				task.ConvertSql.ResetIntervalMinute = XmlUtil.GetXmlAttr(sqlEle, "resetIntervalMinute", 0);
			}
		}

		// 解析ES相关配置（主要用于从ES中读取数据转换）
		if (task.EsAble) {
			Element elasticEle = element.element("elastic");
			if (elasticEle == null) {
				LogHelper.getLogger().error("任务:" + task.TaskId + "配置的EsAble为true，但是却没有配置elastic节点");
				task.DbAble = false;
			} else {
				task.ConvertElastic = new ConvertElastic();
				task.ConvertElastic.setIndex(elasticEle.attribute("index").getText());
				task.ConvertElastic.setType(XmlUtil.GetXmlAttr(elasticEle, "type", "logs"));
				task.ConvertElastic.setSplitType(XmlUtil.GetXmlAttr(elasticEle, "splitType", "day"));
				task.ConvertElastic.setKeyName(XmlUtil.GetXmlAttr(elasticEle, "keyName").getText());
				task.ConvertElastic.setMinTime(XmlUtil.GetXmlAttr(elasticEle, "minTime", 0));
				task.ConvertElastic.setMaxTime(Long.parseLong(XmlUtil.GetXmlAttr(elasticEle, "maxTime").getText()));

				List<ConvertElastic.Filter> filters = new ArrayList<ConvertElastic.Filter>();// task.ConvertElastic.newFilter();
				if (elasticEle.element("filter") != null) {
					for (Object obj : elasticEle.element("filter").elements("item")) {
						ConvertElastic.Filter filter = task.ConvertElastic.newFilter();
						Element eleItem = (Element) obj;
						filter.setCol(XmlUtil.GetXmlAttr(eleItem, "col").getText());
						filter.setVal(XmlUtil.GetXmlAttr(eleItem, "val").getText());
						filters.add(filter);
					}
				}
				task.ConvertElastic.setFilter(filters);

			}
		}
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
						column.Fromfield = ele.attribute("fromfield").getText().toUpperCase();// 转大写
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

					// enableNull
					if (ele.attribute("enableNull") != null)
						column.EnableNull = ele.attribute("enableNull").getText().equalsIgnoreCase("true") ? true : false;
					else
						column.EnableNull = true;

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

	/**
	 * 解析import配置
	 * @param url
	 */
	private static void analysisImport(String url) {
		LogHelper.getLogger().info("准备解析配置文件:" + url);
		File file = new File(url);
		if (!(file.isFile() && file.exists())) {
			throw new RuntimeException("文件:[" + url + "]不存在,绝对路径:" + file.getAbsolutePath());
		}
		try {
			SAXReader reader = new SAXReader();
			Document document = reader.read(file);
			Element root = document.getRootElement();

			if (root.element("firms") != null) {
				for (Object obj : root.element("firms").elements("firm")) {
					Element ele = (Element) obj;
					ConvertFirm cFirm = analysisFirm(ele);

					// 将结果加入到全局配置
					List<ConvertFirm> firms = ConvertFirms.stream().filter(f -> f.FirmId.equals(cFirm.FirmId)).collect(Collectors.toList());
					if (firms.size() == 0) {
						// 不存在的厂商，直接加入
						ConvertFirms.add(cFirm);
					} else {
						ConvertFirm firm = firms.get(0);
						for (ConvertTask task : cFirm.Tasks) {
							if (firm.Tasks.stream().filter(f -> f.TaskId.equals(task.TaskId)).count() == 0) {
								firm.Tasks.add(task);
							} else {
								LogHelper.logger.error("任务编号:" + task.TaskId + " 重复");
							}
						}
					}
				}
			}
		} catch (Exception e) {
			LogHelper.logger.error("解析文件失败:" + url, e);
		}

	}
}
