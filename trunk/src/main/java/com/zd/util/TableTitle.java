package com.zd.util;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zd.config.SystemConfig;

import cn.zdsoft.common.model.DataTable;

public class TableTitle {
	private static DataTable table;

	static {
		String sql = "select code,columns from config_datasource where type='TOPIC' AND code like '%145%'";
		table = new DBAction(SystemConfig.DataBase).getData(sql);
	}

	/**
	 * 根据Topic获得表头部的字符串，用制表符\t分割
	 * @param topic
	 * @return
	 */
	public static String getTitle(String topic) {
		for (int i = 0; i < table.size(); i++) {
			if (table.get(i).get("code").equals(topic)) {
				String cls = table.get(i).get("columns").toString();
				Type type = new TypeToken<List<Map>>() {
				}.getType();
				List<Map> list = new Gson().fromJson(cls, type);

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
		return "";
	}
}
