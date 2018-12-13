package com.zd.config;

import java.util.List;

/**
 * ES数据转换配置，配置和ES索引、类型等相关配置
 * @author 段江涛
 * @date 2018-12-13
 */
public class ConvertElastic {
	public class Filter{
		private String col;
		private String val;
		/**
		 * @return 获取 col
		 */
		public String getCol() {
			return col;
		}
		/**
		 * 设置 col
		 * @param 
		 */
		public void setCol(String col) {
			this.col = col;
		}
		/**
		 * @return 获取 val
		 */
		public String getVal() {
			return val;
		}
		/**
		 * 设置 val
		 * @param 
		 */
		public void setVal(String val) {
			this.val = val;
		}
		
	}

	public Filter newFilter(){
		return new Filter();
	}
	
	private String index;
	private String type;
	//分库类型，可取值为：none,day,month
	private String splitType;
	private String keyName;
	private long minTime;
	private long maxTime;
	private List<Filter> filter;
	
	/**
	 * @return 获取 index
	 */
	public String getIndex() {
		return index;
	}
	/**
	 * 设置 index
	 * @param 
	 */
	public void setIndex(String index) {
		this.index = index;
	}
	/**
	 * @return 获取 type
	 */
	public String getType() {
		return type;
	}
	/**
	 * 设置 type
	 * @param 
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return 获取 splitType
	 */
	public String getSplitType() {
		return splitType;
	}
	/**
	 * 设置 splitType
	 * @param 
	 */
	public void setSplitType(String splitType) {
		this.splitType = splitType;
	}
	/**
	 * @return 获取 keyName
	 */
	public String getKeyName() {
		return keyName;
	}
	/**
	 * 设置 keyName
	 * @param 
	 */
	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}
	/**
	 * @return 获取 minTime
	 */
	public long getMinTime() {
		return minTime;
	}
	/**
	 * 设置 minTime
	 * @param 
	 */
	public void setMinTime(long minTime) {
		this.minTime = minTime;
	}
	/**
	 * @return 获取 maxTime
	 */
	public long getMaxTime() {
		return maxTime;
	}
	/**
	 * 设置 maxTime
	 * @param 
	 */
	public void setMaxTime(long maxTime) {
		this.maxTime = maxTime;
	}
	/**
	 * @return 获取 filter
	 */
	public List<Filter> getFilter() {
		return filter;
	}
	/**
	 * 设置 filter
	 * @param 
	 */
	public void setFilter(List<Filter> filter) {
		this.filter = filter;
	}
	
	
	
}
