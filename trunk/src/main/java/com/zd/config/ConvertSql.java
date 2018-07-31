package com.zd.config;

public class ConvertSql {
	/**
	 * sql脚本，程序将替换@@inttime为当前时间戳
	 */
	public String Sql;
	/**
	 * 主键列名，即替换@@inttime的列名
	 */
	public String KeyName;
	/**
	 * 初始@@inttime的值
	 */
	public int Start;
	/**
	 * 获取或设置几分钟重新全量转换，0或负数表示不全量转换
	 */
	public int ResetIntervalMinute;
}
