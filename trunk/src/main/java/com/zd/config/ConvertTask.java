package com.zd.config;

import java.util.List;
import java.util.Map;

/**
 * 转换任务类
 * @author Administrator
 *
 */
public class ConvertTask {
	/**
	 * 任务编码
	 */
	public String TaskId;
	/**
	 * 主题
	 */
	public String Topic;
	/**
	 * 索引文件路径
	 */
	public String IndexPath;
	/**
	 * 索引文件名称
	 */
	public String IndexName;
	/**
	 * 数据文件路径
	 */
	public String DataPath;
	/**
	 * 数据文件名称
	 */
	public String DataName;
	/**
	 * 转换结果方式:json、xml、zbf(制表符)
	 */
	public String DataType;	
	/**
	 * 筛选条件
	 */
	public Map<String, String> Filter;
	/**
	 * 转换列的集合
	 */
	public List<ConvertColumn> ConvertColumns;

	/**
	 * 是否按区域上报
	 */
	public boolean RegionReport=false;
	/**
	 * 默认区域编码，需要配合RegionReport一起使用
	 */
	public String DefaultRegion="";
	/**
	 * 压缩文件名称
	 */
	public String ZipName="";

	
}
