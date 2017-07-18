package com.zd.config;

import java.util.List;
import java.util.Map;

/**
 * 转换厂商对象
 * 
 * @author 段江涛
 *
 */
public class ConvertFirm {
	/**
	 * 厂商编码
	 */
	public String FirmId;
	/**
	 * 转换任务集合
	 */
	public List<ConvertTask> Tasks;
	/**
	 * 字典转换对象，用于对应爱思和第三方厂商的字典信息
	 */
	public Map<String,List<ConvertDiction>> ConvertDictions;

}
