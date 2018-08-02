package com.zd.config;

import com.zd.config.model.FilterType;

/**
 * 筛选对象
 * @author 段江涛
 * @date 2018-08-02
 */
public class ConvertFilter {
	private String key;		//列名
	private String value;	//根据类型对应的值
	private FilterType type;//类型
			
	public ConvertFilter() {
		this.type=FilterType.VALUE;
	}
	
	public ConvertFilter(String key, String value, FilterType type) {
		this.key = key;
		this.value = value;
		this.type = type;
	}



	/**
	 * @return 获取 key
	 */
	public String getKey() {
		return key;
	}
	/**
	 * 设置 key
	 * @param 
	 */
	public void setKey(String key) {
		this.key = key;
	}
	/**
	 * @return 获取 value
	 */
	public String getValue() {
		return value;
	}
	/**
	 * 设置 value
	 * @param 
	 */
	public void setValue(String value) {
		this.value = value;
	}
	/**
	 * @return 获取 type
	 */
	public FilterType getType() {
		return type;
	}
	/**
	 * 设置 type
	 * @param 
	 */
	public void setType(FilterType type) {
		this.type = type;
	}
	
	
}
