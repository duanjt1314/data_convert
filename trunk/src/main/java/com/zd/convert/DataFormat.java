package com.zd.convert;

import java.util.List;
import java.util.Map;

import com.zd.config.ConvertDiction;

import cn.zdsoft.common.model.DataRow;

/**
 * 数据格式化
 * 
 * @author Administrator
 *
 */
public class DataFormat {
	/**
	 * 格式化方法，格式如下：toUpperCase(substring(row[SITE_ID],0,3))
	 * 
	 * @param row
	 *            待格式化的这一行数据
	 * @param format
	 *            格式化方法字符串
	 * @return
	 */
	public static String Format(Map<String, List<ConvertDiction>> dicMap, DataRow row, String format) {
		int start = format.indexOf("(");
		int end = format.lastIndexOf(")");
		// 截取方法名
		String funName = format.substring(0, start);
		String content = format.substring(start + 1, end);

		if (content.indexOf("(") >= 0 && content.indexOf(")") >= 0) {
			// 包含一对括号，继续递归
			content = Format(dicMap, row, content);
		}

		// 判断是从DataRow里面取值还是直接取值
		String[] param = content.split(",");
		String val = param[0];
		if (val.indexOf("row[") >= 0 && val.indexOf("]") >= 0) {
			int valBeginIndex = val.indexOf("[");
			int valEndIndex = val.lastIndexOf("]");
			String columnName = val.substring(valBeginIndex + 1, valEndIndex);
			if (!row.containsKey(columnName)) {
				throw new RuntimeException("自定义函数substring,数据中不包含列:" + columnName);
			}
			val = row.get(columnName).toString();
		}

		switch (funName) {
		case "substring":// 截取字符串,三个参数
			if (param.length != 3) {
				throw new RuntimeException("自定义函数substring必须有3个参数");
			}
			return val.substring(Integer.parseInt(param[1]), Integer.parseInt(param[2]));

		case "toUpperCase":// 转大写
			if (param.length != 1) {
				throw new RuntimeException("自定义函数substring必须有1个参数");
			}
			return val.toUpperCase();
		case "toLowerCase":// 转小写
			if (param.length != 1) {
				throw new RuntimeException("自定义函数substring必须有1个参数");
			}
			return val.toLowerCase();
		case "dicConvert":// 字典信息转换 dicConvert(row[ID_DICT_VTYPE],virtual,200)
			if (param.length != 3) {
				throw new RuntimeException("自定义函数dicConvert必须有3个参数");
			}
			if (dicMap.containsKey(param[1])) {
				for (ConvertDiction dict : dicMap.get(param[1])) {
					if(dict.Val.equals(val)){
						return dict.Export;
					}
				}
				return param[2];
			} else {
				return param[2];
			}
		default:
			return val;
		}
	}

}
