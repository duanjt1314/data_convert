package com.zd.config;

import java.util.ArrayList;
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
	public boolean RegionReport = false;
	/**
	 * 默认区域编码，需要配合RegionReport一起使用
	 */
	public String DefaultRegion = "";
	/**
	 * 压缩文件名称
	 */
	public String ZipName = "";
	/**
	 * 需要解析的文件类型，即后缀名
	 */
	public String FileType = "";
	/**
	 * 是否压缩
	 */
	public boolean HasCompress = true;
	/**
	 * 是否包含所有文件
	 */
	public boolean HasIndex = true;

	/**
	 * 需要解析的文件名内容。只要文件名里面包含了该内容就会被解析
	 */
	public List<String> SearchPatterns = new ArrayList<String>();

	// 下面是用于数据保障的三列配置
	public String SiteIdName = "";
	public String DeviceIdName = "";
	public String SourceSiteIdName = "";

}
