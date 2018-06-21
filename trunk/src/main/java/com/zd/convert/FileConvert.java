package com.zd.convert;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.zd.config.ConvertColumn;
import com.zd.config.ConvertFirm;
import com.zd.config.ConvertTask;
import com.zd.config.SystemConfig;
import com.zd.util.Helper;
import com.zd.util.LogHelper;

import cn.zdsoft.common.*;
import cn.zdsoft.common.model.*;

/**
 * 文件转换对象。将数据转换为文件
 * 
 * @author Administrator
 *
 */
public class FileConvert {

	/**
	 * 转换的列的集合
	 */
	private ConvertTask ConvertTask;

	/**
	 * 厂商信息
	 */
	private ConvertFirm FirmInfo;
	/**
	 * 定义唯一转换编码，方便记录并查看日志
	 */
	private String ConvertId;

	/**
	 * 实例化转换线程程序
	 * 
	 * @param convertTask
	 *            转换任务，包含任务的所有属性
	 * @param firmInfo
	 *            厂商编码，表示当前任务属于哪个厂商
	 * @param convertId
	 *            转换唯一编码，方便查看日志，排查问题
	 */
	public FileConvert(com.zd.config.ConvertTask convertTask, ConvertFirm firmInfo, String convertId) {
		super();
		ConvertTask = convertTask;
		FirmInfo = firmInfo;
		ConvertId = convertId;
	}

	/**
	 * 文件处理
	 * 
	 * @param content
	 *            从消息队列上拉取的字符串消息
	 * @return
	 */
	public void DealFile(String content) {
		DataTable dataTable = StringUtil.String2Map(content);
		if (dataTable == null || dataTable.size() == 0) {
			LogHelper.getLogger().error(GetLogPrefix() + "从消息队列拉取的消息转换后集合为空,content:" + content);
			return;
		}

		// 确保目录存在
		CreateDir();
		// Filter筛选数据
		DataTable list = FilterData(dataTable);
		if (list.size() == 0) {
			LogHelper.getLogger().info(GetLogPrefix() + "数据清理后为空，不进行文件处理。原数据是:" + System.lineSeparator() + content);
			return;
		}

		if (ConvertTask.RegionReport) {
			if (list.get(0).containsKey("REGION_ID") || list.get(0).containsKey("SITE_ID")) {
				// 可以分组
				Map<String, DataTable> mapList = new HashMap<String, DataTable>();
				for (DataRow row : list) {
					String regionId = row.get("SITE_ID").toString().substring(0, 6);
					if (mapList.containsKey(regionId)) {
						DataTable dt = mapList.get(regionId);
						dt.add(row);
						mapList.put(regionId, dt);
					} else {
						DataTable dt = new DataTable();
						dt.add(row);
						mapList.put(regionId, dt);
					}
				}

				// 循环转换
				for (String key : mapList.keySet()) {
					WriteFile(DealFile(mapList.get(key)), key);
				}

			} else {
				// 无法分组,但是可以传入默认区域编码
				WriteFile(DealFile(list), ConvertTask.DefaultRegion);
			}

		} else {
			// 直接上报
			WriteFile(DealFile(list), "");
		}

		LogHelper.getLogger().debug(GetLogPrefix() + "转换完成。源数据:" + System.lineSeparator() + content);
	}

	/**
	 * 创建相关目录
	 */
	private void CreateDir() {
		File errFile = new File(SystemConfig.ErrorDir);
		if (!errFile.exists() || errFile.isFile()) {
			errFile.mkdir();
		}

		File outFile = new File(SystemConfig.OutputDir);
		if (!outFile.exists() || outFile.isFile()) {
			outFile.mkdir();
		}

		File tempFile = new File(SystemConfig.TempDir);
		if (!tempFile.exists() || tempFile.isFile()) {
			tempFile.mkdir();
		}
	}

	/**
	 * 根据Filter的配置筛选需要的数据
	 * 
	 * @param dt
	 * @return
	 */
	private DataTable FilterData(DataTable dt) {
		if (ConvertTask.Filter != null && ConvertTask.Filter.size() > 0) {
			DataTable result = new DataTable();
			for (DataRow row : dt) {
				boolean isAdd = true;// 是否新增
				for (String key : ConvertTask.Filter.keySet()) {
					if (!row.get(key).toString().equals(ConvertTask.Filter.get(key))) {
						// 不提取
						isAdd = false;
						break;
					}
				}
				if (isAdd) {
					result.add(row);
				}
			}
			return result;
		} else {
			return dt;
		}
	}

	/**
	 * 将原始数据转换为目标数据的Map键值对集合
	 * 
	 * @param list
	 *            原始键值对集合
	 * @return 目标键值对集合
	 */
	private DataTable DealFile(DataTable list) {
		DataTable result = new DataTable();
		for (DataRow map : list) {
			DataRow row = new DataRow();
			for (ConvertColumn convertColumn : ConvertTask.ConvertColumns) {
				Object data = null;
				if (!StringUtil.IsNullOrEmpty(convertColumn.Formate)) {
					// 格式化
					try {
						data = DataFormat.Format(FirmInfo.ConvertDictions, map, convertColumn.Formate);
					} catch (Exception ex) {
						LogHelper.getLogger().error("格式化失败.格式化内容:" + convertColumn.Formate
								+System.lineSeparator()+"源数据:"+StringUtil.GetJsonString(map), ex);
					}
				} else {
					if (map.containsKey(convertColumn.Fromfield)) {
						data = map.get(convertColumn.Fromfield);
					} else {
						data = convertColumn.DefaultValue;// 默认值
					}
				}
				row.put(convertColumn.Tofield, data);
			}
			result.add(row);
		}
		return result;

	}

	/**
	 * 写入文件
	 * 
	 * @param list
	 */
	private void WriteFile(DataTable list, String regionId) {
		String path = "";
		try {
			// 创建目录
			path = PathUtil.Combine(SystemConfig.TempDir, UUID.randomUUID().toString());
			File file = new File(path);
			if (!file.exists())
				file.mkdirs();// 创建目录

			// 替换并记录所有文件名称
			String indexFileContent = FileUtil.ReadAllString(PathUtil.Combine(Helper.GetAppDir(), ConvertTask.IndexPath));
			String dataFileName = FileNameReplace(ConvertTask.DataName, "", list.size() + "", regionId);
			String indexFileName = FileNameReplace(ConvertTask.IndexName, dataFileName, list.size() + "", regionId);
			String zipName = FileNameReplace(ConvertTask.ZipName, dataFileName, list.size() + "", regionId);
			indexFileContent = FileNameReplace(indexFileContent, dataFileName, list.size() + "", regionId);

			boolean res = WriteIndexFile(PathUtil.Combine(path, indexFileName), indexFileContent);
			if (!res) {
				String fileName = WriteErrorFile(list);
				LogHelper.getLogger().error(GetLogPrefix() + "写入索引文件错误，任务:" + ConvertTask.TaskId + " 厂商:" + FirmInfo + ".错误文件存放到:" + fileName);
				return;
			}
			res = WriteDataFile(list, PathUtil.Combine(path, dataFileName));
			if (!res) {
				String fileName = WriteErrorFile(list);
				LogHelper.getLogger().error(GetLogPrefix() + "写入数据文件错误，任务:" + ConvertTask.TaskId + " 厂商:" + FirmInfo + ".错误文件存放到:" + fileName);
				return;
			}

			String targetPath = PathUtil.Combine(SystemConfig.OutputDir, FirmInfo.FirmId);
			ZipUtil.Zip(path, targetPath, zipName, false);
			LogHelper.getLogger().info(String.format(GetLogPrefix() + "文件转换成功，转换后的路径是:%s", //
					PathUtil.Combine(targetPath, zipName)));
		} catch (Exception e) {
			LogHelper.getLogger().error(GetLogPrefix() + "写入文件出现异常，参数:" + StringUtil.GetJsonString(list), e);
		} finally {
			// 删除目录
			if (!StringUtil.IsNullOrEmpty(path)) {
				LogHelper.getLogger().info(GetLogPrefix() + "准备删除目录:" + path);
				if (!FileUtil.DeleteDir(path)) {
					LogHelper.getLogger().error(GetLogPrefix() + "删除目录失败:" + path);
				}
			}
		}
	}

	/**
	 * 写入数据文件
	 * 
	 * @param list
	 *            待写入的数据
	 * @param url
	 *            写入路径
	 * @return 是否写入成功
	 */
	private boolean WriteDataFile(DataTable list, String url) {
		try {
			String content = StringUtil.Map2String(list, false);
			FileUtil.WriteAllString(url, content);
			return true;
		} catch (Exception e) {
			LogHelper.getLogger().error(GetLogPrefix() + "写入文件" + url + "出现错误", e);
			return false;
		}
	}

	/**
	 * 写入索引文件
	 * 
	 * @param url
	 *            索引文件绝对路径
	 * @param content
	 *            写入索引文件的内容
	 * @return
	 */
	private boolean WriteIndexFile(String url, String content) {
		try {
			FileUtil.WriteAllString(url, content);
			return true;
		} catch (Exception e) {
			LogHelper.getLogger().error(GetLogPrefix() + "写入文件" + url + "出现错误", e);
			return false;
		}
	}

	/**
	 * 写入错误文件并返回错误文件路径
	 * 
	 * @param list
	 * @return
	 * @throws IOException
	 */
	private String WriteErrorFile(DataTable list) throws IOException {
		// 创建目录
		String path = PathUtil.Combine(SystemConfig.ErrorDir, FirmInfo.FirmId);
		File file = new File(path);
		if (!(file.exists() && file.isDirectory())) {
			file.mkdir();
		}
		String fileName = PathUtil.Combine(path, UUID.randomUUID().toString() + ".log");
		FileUtil.WriteAllString(fileName, StringUtil.GetJsonString(list));
		return fileName;
	}

	/**
	 * 指定替换
	 * 
	 * @param content
	 *            待替换的字符串
	 * @param fileName
	 *            文件名
	 * @param dataCount
	 *            文件总数量
	 * @param regionId
	 *            区域编码
	 * @return
	 */
	private String FileNameReplace(String content, String fileName, String dataCount, String regionId) {
		content = content.replace("@@regionid", regionId);
		content = content.replace("@@datacount", dataCount);
		content = content.replace("@@filename", fileName);
		content = content.replace("@@timeint", Helper.GetTimeInt() + "");// 时间戳
		content = content.replace("@@timestring", Helper.GetTimeString());// 时间字符串
		content = content.replace("@@sequence5", Helper.getSequence5());// 五位自增数
		return content;
	}

	/**
	 * 获取日志信息前缀，包含转换唯一编码，任务编码，厂商编码
	 * 
	 * @return
	 */
	private String GetLogPrefix() {
		return "转换唯一编码:" + this.ConvertId + System.lineSeparator()//
				+ "厂商编码:" + this.FirmInfo.FirmId + System.lineSeparator()//
				+ "任务编码:" + this.ConvertTask.TaskId + System.lineSeparator();
	}
}
