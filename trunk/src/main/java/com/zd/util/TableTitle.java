package com.zd.util;

import java.io.File;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.zd.config.SystemConfig;

import cn.zdsoft.common.model.DataTable;

public class TableTitle {
	private static DataTable table;
	private static Config config;

	static {
		try {
			String sql = "select code,columns from config_datasource where type='TOPIC' ";
			table = new DBAction(SystemConfig.DataBase).getData(sql);

			if (table == null || table.size() == 0) {
				String msg = "查询表config_datasource数据失败,将从config/topic-list.properties下读取数据\r\n";
				msg += "如果没有该文件,可创建该文件并通过脚本:select CONCAT(`code`,'=',columns) from bdplatform.config_datasource where type='TOPIC'获取数据";
				LogHelper.logger.warn(msg);

				Resource resource = new ClassPathResource("config" + File.separator + "topic-list.properties");
				if (resource.exists()) {
					Config _config = ConfigFactory.parseFile(resource.getFile());
					config = _config;
					LogHelper.logger.info("找到并加载了文件:" + resource.getURI());
				} else {
					LogHelper.logger.error("未找到文件:" + resource.getURI());
				}

			}
		} catch (Exception ex) {
			LogHelper.logger.error("", ex);
		}
	}

	/**
	 * 根据Topic获得表头部的字符串，用制表符\t分割
	 * @param topic
	 * @return
	 */
	public static String getTitle(String topic) {
		String columns = "";
		if (config != null) {
			columns = config.getString(topic);
		} else {
			for (int i = 0; i < table.size(); i++) {
				if (table.get(i).get("code").equals(topic)) {
					columns = table.get(i).get("columns").toString();
					break;
				}
			}
		}

		Type type = new TypeToken<List<Map>>() {
		}.getType();
		List<Map> list = new Gson().fromJson(columns, type);

		String result = "";
		for (Map map : list) {
			if (result.equals("")) {
				result += map.get("column").toString();
			} else {
				result += "\t" + map.get("column").toString();
			}
		}
		return result;
	}
}
