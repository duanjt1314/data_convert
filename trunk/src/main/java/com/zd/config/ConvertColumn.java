package com.zd.config;

/**
 * 转换列的配置
 * 
 * @author 段江涛
 *
 */
public class ConvertColumn {
	/**
	 * 唯一标识,键
	 */
	public String Key;
	/**
	 * 中文描述
	 */
	public String Chn;
	/**
	 * 来源字段名称
	 */
	public String Fromfield;
	/**
	 * 转换后字段名称
	 */
	public String Tofield;
	/**
	 * 默认值
	 */
	public String DefaultValue;
	/**
	 * 格式化方法配置
	 */
	public String Formate;
	/**
	 * 是否允许为空
	 */
	public Boolean EnableNull;
	
}
