package com.zd.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dom4j.Attribute;
import org.dom4j.Branch;
import org.dom4j.CDATA;
import org.dom4j.Comment;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Entity;
import org.dom4j.InvalidXPathException;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.ProcessingInstruction;
import org.dom4j.QName;
import org.dom4j.Text;
import org.dom4j.Visitor;
import org.dom4j.XPath;
import org.dom4j.io.SAXReader;
import org.dom4j.io.SAXWriter;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.BaseElement;

import com.zd.config.model.TaskPro;
import com.zd.util.LogHelper;

/**
 * 任务进度类，用于保存或获取任务当前的进度。
 * 数据存放在config/process.xml中
 * @author 段江涛
 * @date 2018-07-31
 */
public class ProcessTask {
	private static String fileName = Paths.get("config", "process.xml").toString();
	private static List<TaskPro> taskPros;
	private static Object lockObj = new Object();

	private static void loadFromXml() {
		taskPros = new ArrayList<TaskPro>();
		if (new File(fileName).exists()) {
			try {
				SAXReader reader = new SAXReader();
				File f = new File(fileName);
				Document document = reader.read(f);
				Element root = document.getRootElement();

				for (Object obj : root.elements("taskPro")) {
					Element rootEle = (Element) obj;
					TaskPro taskPro = new TaskPro();
					taskPro.setFirmId(rootEle.attribute("firmId").getText());
					taskPro.setTaskId(rootEle.attribute("taskId").getText());
					taskPro.setProValue(Integer.parseInt(rootEle.attribute("proValue").getText()));
					taskPros.add(taskPro);
				}
				LogHelper.getLogger().info("成功加载进度文件process.xml，总条数:" + taskPros.size());
			} catch (Exception ex) {
				LogHelper.getLogger().error("加载进度文件失败,文件名:" + fileName, ex);
			}
		} else {
			LogHelper.getLogger().warn("未找到进度文件:" + fileName);
		}
	}

	/**
	 * 获取任务进度
	 * @param firmId
	 * @param taskId
	 * @return
	 */
	public static int getProValue(String firmId, String taskId) {
		try {
			// 必须使用双层lock
			if (taskPros == null) {
				synchronized (lockObj) {
					if (taskPros == null) {
						loadFromXml();
					}
				}
			}

			synchronized (lockObj) {
				List<TaskPro> list = taskPros.stream().filter(f -> f.getFirmId().equals(firmId) && //
						f.getTaskId().equals(taskId)).collect(Collectors.toList());
				if (list.size() > 0) {
					return list.get(0).getProValue();
				} else {
					LogHelper.getLogger().info("DestId:" + firmId + ",TaskId:" + taskId + "未找到数据");
					return 0;
				}
			}
		} catch (Exception ex) {
			LogHelper.getLogger().error("获取进度失败", ex);
			return 0;
		}
	}

	/**
	 * 保存进度
	 * @param taskPro
	 */
	public static void saveProValue(TaskPro taskPro) {
		try {
			synchronized (lockObj) {
				List<TaskPro> list = taskPros.stream().filter(f -> f.getFirmId().equals(taskPro.getFirmId()) && //
						f.getTaskId().equals(taskPro.getTaskId())).collect(Collectors.toList());

				if (list.size() > 0) {
					taskPros.remove(list.get(0));
				}
				taskPros.add(taskPro);
				saveToXml();
			}
		} catch (Exception ex) {
			LogHelper.getLogger().error("保存进度失败", ex);
		}
	}

	private static void saveToXml() {
		try {
			Document document = DocumentHelper.createDocument();
			Element root = DocumentHelper.createElement("student");
			document.setRootElement(root);

			for (TaskPro taskPro : taskPros) {
				Element ele = new BaseElement("taskPro");
				ele.addAttribute("firmId", taskPro.getFirmId());
				ele.addAttribute("taskId", taskPro.getTaskId());
				ele.addAttribute("proValue", taskPro.getProValue() + "");
				root.add(ele);
			}

			XMLWriter xmlWriter = new XMLWriter(new FileWriter(fileName));
			xmlWriter.write(document);
			xmlWriter.flush();

		} catch (Exception ex) {
			LogHelper.getLogger().error("保存进度文件失败,文件名:" + fileName, ex);
		}
	}
}
