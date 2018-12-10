package com.zd.convert;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.zd.config.ConvertDiction;
import com.zd.util.LogHelper;

import cn.zdsoft.common.DateUtil;
import cn.zdsoft.common.StringUtil;
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
		// 首先判断是否存在加号(+),如果存在就将几个数据加起来
		if (format.contains("+")) {
			String[] items = format.split("\\+");
			String res = "";
			for (int i = 0; i < items.length; i++) {
				res += Format(dicMap, row, items[i]);
			}
			return res;
		}

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

		switch (funName.toLowerCase()) {
		case "substring":// 截取字符串,三个参数
			if (param.length != 3) {
				throw new RuntimeException("自定义函数substring必须有3个参数");
			}
			return val.substring(Integer.parseInt(param[1]), Integer.parseInt(param[2]));

		case "touppercase":// 转大写
			if (param.length != 1) {
				throw new RuntimeException("自定义函数substring必须有1个参数");
			}
			return val.toUpperCase();
		case "tolowercase":// 转小写
			if (param.length != 1) {
				throw new RuntimeException("自定义函数substring必须有1个参数");
			}
			return val.toLowerCase();
		case "dicconvert":// 字典信息转换 dicConvert(row[ID_DICT_VTYPE],virtual,200)
			if (param.length != 3) {
				throw new RuntimeException("自定义函数dicConvert必须有3个参数");
			}
			if (dicMap.containsKey(param[1])) {
				for (ConvertDiction dict : dicMap.get(param[1])) {
					if (dict.Val.equals(val)) {
						return dict.Export;
					}
				}
				return param[2];
			} else {
				return param[2];
			}
		case "macclear":// 将MAC地址中间的横线和冒号去掉
			if (param.length != 1) {
				throw new RuntimeException("自定义函数macClear必须有1个参数");
			}
			return val.replace("-", "").replaceAll(":", "");
		case "formatmac":// 将MAC格式化为标准的以横线分割的，全大写的格式
			if (param.length != 1) {
				throw new RuntimeException("自定义函数formatMac必须有1个参数");
			}
			return formatMac(val);
		case "iptolong":// 将IP地址转换为整数类型
			if (param.length != 1) {
				throw new RuntimeException("自定义函数ipToLong必须有1个参数");
			}
			return ipToLong(val) + "";
		case "longtoip":// 将整数型的Ip转换为ip格式
			if (param.length != 1) {
				throw new RuntimeException("自定义函数longToIp必须有1个参数");
			}
			boolean con = Pattern.matches("\\d*", val);
			if (!con) {
				LogHelper.logger.error("longToIp参数必须为整数:" + val);
				return "";
			} else {
				int ip = Integer.parseInt(val);
				return getIP(ip);
			}
		case "longtodatetime"://将时间戳转换为日期格式yyyy-MM-dd HH:mm:ss
			if (param.length != 1) {
				throw new RuntimeException("自定义函数longToDateTime必须有1个参数");
			}
			con = Pattern.matches("\\d*", val);
			if (!con) {
				LogHelper.logger.error("longToDateTime参数必须为整数:" + val);
				return "";
			} else {
				int time = Integer.parseInt(val);
				return longToDate(time,"yyyy-MM-dd HH:mm:ss");
			}			
		default:
			return val;
		}
	}

	/**
	 * 判断文件是否被占用
	 * @param file
	 * @return
	 */
	private static boolean isFileInUse(File file) {
		if (file.renameTo(file)) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * IP转long
	 * @param ip
	 * @return
	 */
	private static long ipToLong(String ip) {
		if (StringUtil.IsNullOrEmpty(ip)) {
			return 0;
		}
		String[] ipArray = ip.split("\\.");
		List ipNums = new ArrayList();
		for (int i = 0; i < 4; ++i) {
			ipNums.add(Long.valueOf(Long.parseLong(ipArray[i].trim())));
		}
		long ZhongIPNumTotal = ((Long) ipNums.get(0)).longValue() * 256L * 256L * 256L + ((Long) ipNums.get(1)).longValue() * 256L * 256L + ((Long) ipNums.get(2)).longValue() * 256L + ((Long) ipNums.get(3)).longValue();

		return ZhongIPNumTotal;
	}

	/**
	 * long转ip
	 * @param ipaddr
	 * @return
	 */
	private static String getIP(long ipaddr) {
		long y = ipaddr % 256;
		long m = (ipaddr - y) / (256 * 256 * 256);
		long n = (ipaddr - 256 * 256 * 256 * m - y) / (256 * 256);
		long x = (ipaddr - 256 * 256 * 256 * m - 256 * 256 * n - y) / 256;
		return m + "." + n + "." + x + "." + y;
	}
	
	/**
	 * 将时间戳转换为时间字符串
	 * @param time 时间戳，秒数
	 * @param format 格式化字符串
	 * @return 按照指定格式化字符串生成的字符串
	 */
	private static String longToDate(long time,String format){
		Date date= new Date(time*1000);
		return DateUtil.Format(date, format);
	}
	
	/**
	 * 格式化MAC地址，将MAC地址格式化成：00-E0-4C-3B-7D-2F
	 * @param mac
	 * @return
	 */
	private static String formatMac(String mac){
		if(StringUtil.IsNullOrEmpty(mac)){
			return "";
		}
		mac = mac.replace("-", "").replaceAll(":", "").toUpperCase();
		if (mac.length() != 12) {
			LogHelper.logger.error("格式化方法formatMac错误，参数格式不正确:" + mac);
			return "";
		}
		String result = mac.substring(0, 2) + "-"//
				+ mac.substring(2, 4) + "-"//
				+ mac.substring(4, 6) + "-"//
				+ mac.substring(6, 8) + "-"//
				+ mac.substring(8, 10) + "-"//
				+ mac.substring(10) + "-";
		return result;
	}
}
