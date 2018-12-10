package com.zd.convert;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.zd.config.ConvertColumn;
import com.zd.config.ConvertFilter;
import com.zd.config.ConvertFirm;
import com.zd.config.ConvertTask;
import com.zd.config.SystemConfig;
import com.zd.kafka.JavaKafkaProducer;
import com.zd.util.Helper;
import com.zd.util.Kafka;
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
	 * 源文件名，用于数据保障
	 */
	private String SourceFileName;

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
		DealFile(dataTable, "");
	}

	public void DealFile(DataTable dataTable, String fileName) {
		this.SourceFileName = fileName;

		// 确保目录存在
		CreateDir();
		// Filter筛选数据
		DataTable list = FilterData(dataTable);
		if (list.size() == 0) {
			LogHelper.getLogger().info(GetLogPrefix() + "数据清理后为空，不进行文件处理。");
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
					WriteFile(ConvertData(mapList.get(key)), key);
				}

			} else {
				// 无法分组,但是可以传入默认区域编码
				WriteFile(ConvertData(list), ConvertTask.DefaultRegion);
			}

		} else {
			// 直接上报
			WriteFile(ConvertData(list), "");
		}

		LogHelper.getLogger().debug(GetLogPrefix() + "转换完成。");
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
		if (dt.isNullOrEmpty()) {
			return dt;
		}

		String[] columns = dt.getColumns();

		if (ConvertTask.Filter != null && ConvertTask.Filter.size() > 0) {
			DataTable result = new DataTable();// 定义返回集合
			for (DataRow row : dt) {
				boolean isAdd = true;// 是否新增

				for (String col : columns) {
					try {
						if (ConvertTask.Filter.containsKey(col)) {
							for (ConvertFilter filter : ConvertTask.Filter.get(col)) {
								// 不满足条件的就不需要新增
								switch (filter.getType()) {
								case VALUE: {
									if (!row.get(col).toString().equals(filter.getValue())) {
										isAdd = false;
									}
									break;
								}
								case NOTVALUE: {
									if (row.get(col).toString().equals(filter.getValue())) {
										isAdd = false;
									}
									break;
								}
								case EXP: {
									if (!Pattern.matches(filter.getValue(), row.get(col).toString())) {
										isAdd = false;
									}
									break;
								}
								}
							}

						}
					} catch (Exception ex) {
						String errMsg = "清理数据异常,\r\ndata:" + new Gson().toJson(row)//
								+ "\r\nclear:" + new Gson().toJson(ConvertTask.Filter);
						LogHelper.getLogger().error(errMsg, ex);
						isAdd = false;
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
	private DataTable ConvertData(DataTable list) {
		DataTable result = new DataTable();
		for (DataRow map : list) {
			DataRow row = new DataRow();
			for (ConvertColumn convertColumn : ConvertTask.ConvertColumns) {
				Object data = "";
				if (!StringUtil.IsNullOrEmpty(convertColumn.Formate)) {
					// 格式化
					try {
						data = DataFormat.Format(FirmInfo.ConvertDictions, map, convertColumn.Formate);
					} catch (Exception ex) {
						LogHelper.getLogger()
								.error("格式化失败.格式化内容:" + convertColumn.Formate + //
										"topic:" + ConvertTask.Topic + //
										System.lineSeparator() + "源数据:" + StringUtil.GetJsonString(map), ex);
					}
				} else {
					if (map.containsKey(convertColumn.Fromfield)) {
						data = map.get(convertColumn.Fromfield);
					} else {
						data = convertColumn.DefaultValue;// 默认值
					}
				}

				// 默认值赋值
				if (!convertColumn.EnableNull && StringUtil.IsNullOrEmpty(data.toString())) {
					data = convertColumn.DefaultValue;
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
		if (ConvertTask.HasCompress) {
			WriteFileCompress(list, regionId);
		} else {
			WriteFileNoCompress(list, regionId);
		}
	}

	/**
	 * 写入文件-需要压缩
	 * @param list
	 * @param regionId
	 */
	private void WriteFileCompress(DataTable list, String regionId) {
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
			res = WriteDataFile(list.copy(), PathUtil.Combine(path, dataFileName));
			if (!res) {
				String fileName = WriteErrorFile(list);
				LogHelper.getLogger().error(GetLogPrefix() + "写入数据文件错误，任务:" + ConvertTask.TaskId + " 厂商:" + FirmInfo + ".错误文件存放到:" + fileName);
				return;
			}

			String targetPath = PathUtil.Combine(SystemConfig.OutputDir, FirmInfo.FirmId);
			ZipUtil.Zip(path, targetPath, zipName, false);
			LogHelper.getLogger().info(String.format(GetLogPrefix() + "文件转换成功，转换后的路径是:%s", //
					PathUtil.Combine(targetPath, zipName)));

			// 数据质量
			WriteTransLog(list, zipName);
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
	 * 写入文件，不需要压缩。直接把文件写入输出目录
	 * @param list
	 * @param regionId
	 */
	private void WriteFileNoCompress(DataTable list, String regionId) {
		String path = "";
		try {
			path = PathUtil.Combine(SystemConfig.OutputDir, FirmInfo.FirmId);
			File file = new File(path);
			if (!file.exists())
				file.mkdirs();// 创建目录

			String dataFileName = FileNameReplace(ConvertTask.DataName, "", list.size() + "", regionId);

			boolean res = true;
			if (ConvertTask.HasIndex) {
				String indexFileContent = FileUtil.ReadAllString(PathUtil.Combine(Helper.GetAppDir(), ConvertTask.IndexPath));
				String indexFileName = FileNameReplace(ConvertTask.IndexName, dataFileName, list.size() + "", regionId);
				res = WriteIndexFile(PathUtil.Combine(path, indexFileName), indexFileContent);
				if (res) {
					LogHelper.getLogger().info("索引文件写入成功:" + PathUtil.Combine(path, indexFileName));
				}
			}
			if (!res) {
				String fileName = WriteErrorFile(list);
				LogHelper.getLogger().error(GetLogPrefix() + "写入索引文件错误，任务:" + ConvertTask.TaskId + " 厂商:" + FirmInfo + ".错误文件存放到:" + fileName);
				return;
			}

			res = WriteDataFile(list.copy(), PathUtil.Combine(path, dataFileName));
			if (!res) {
				String fileName = WriteErrorFile(list);
				LogHelper.getLogger().error(GetLogPrefix() + "写入数据文件错误，任务:" + ConvertTask.TaskId + " 厂商:" + FirmInfo + ".错误文件存放到:" + fileName);
				return;
			} else {
				LogHelper.getLogger().info(String.format(GetLogPrefix() + "数据文件写入成功:%s", //
						PathUtil.Combine(path, dataFileName)) + ",总条数：" + list.size());
			}

			// 数据质量
			WriteTransLog(list, PathUtil.Combine(path, dataFileName));
		} catch (Exception e) {
			LogHelper.getLogger().error(GetLogPrefix() + "写入文件出现异常，参数:" + StringUtil.GetJsonString(list), e);
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
			// 数据保障包含的列要删除
			for (DataRow dataRow : list) {
				if (!StringUtil.IsNullOrEmpty(ConvertTask.SiteIdName)) {
					dataRow.remove(ConvertTask.SiteIdName);
				}
				if (!StringUtil.IsNullOrEmpty(ConvertTask.DeviceIdName)) {
					dataRow.remove(ConvertTask.DeviceIdName);
				}
				if (!StringUtil.IsNullOrEmpty(ConvertTask.SourceSiteIdName)) {
					dataRow.remove(ConvertTask.SourceSiteIdName);
				}
			}

			// 写入文件
			if (ConvertTask.DataType.equals("zbf-gz")) {
				String content = StringUtil.Map2String(list, true);
				GZipUtil.Compress(content, url);
				return true;
			} else {// 默认是zbf
				String content = StringUtil.Map2String(list, false);
				FileUtil.WriteAllString(url, content);
				return true;
			}

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
	 * 写入数据保障，推送到kafka
	 * @param zipName
	 */
	private void WriteTransLog(DataTable data, String zipName) {
		try {
			if (!StringUtil.IsNullOrEmpty(ConvertTask.SiteIdName)//
					&& !StringUtil.IsNullOrEmpty(ConvertTask.DeviceIdName)//
					&& !StringUtil.IsNullOrEmpty(ConvertTask.SourceSiteIdName)) {
				String key = "";
				Map<String, Integer> map = new HashMap<String, Integer>();
				for (DataRow dataRow : data) {
					key = dataRow.get(ConvertTask.SiteIdName) + "&"//
							+ dataRow.get(ConvertTask.DeviceIdName) + "&"//
							+ dataRow.get(ConvertTask.SourceSiteIdName);
					int count = 1;
					if (map.containsKey(key)) {
						count = map.get(key) + 1;
					}
					map.put(key, count);
				}

				// 推送到kafka
				for (String mk : map.keySet()) {
					String siteId = mk.split("&")[0];
					String deviceId = mk.split("&")[1];
					String sourceSiteId = mk.split("&")[2];

					List<String> dataArr = new ArrayList<String>();
					long tran_date = new Date().getTime() / 1000;
					dataArr.add(CheckSumId.GetId(tran_date, siteId, deviceId, sourceSiteId));
					dataArr.add(siteId);
					dataArr.add(deviceId);
					dataArr.add(FirmInfo.FirmId);
					dataArr.add(tran_date + "");
					dataArr.add(ConvertTask.TaskId);
					dataArr.add(SourceFileName);
					dataArr.add(zipName);
					dataArr.add(map.get(mk) + "");// 数量
					dataArr.add(sourceSiteId);

					Kafka.SendTransKafka(String.join("\t", dataArr));
				}
			}
		} catch (Exception e) {
			String msg = "写入数据保障错误,data:" + StringUtil.GetJsonString(data)//
					+ "\r\nzipName:" + zipName//
					+ "\r\nTaskId:" + ConvertTask.TaskId;
			LogHelper.getLogger().error(msg, e);
		}
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
