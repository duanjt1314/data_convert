package com.zd.kafka;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dom4j.DocumentException;

import com.zd.config.ConvertFirm;
import com.zd.config.ConvertTask;
import com.zd.config.SystemConfig;
import com.zd.convert.FileConvert;
import com.zd.util.Helper;
import com.zd.util.LogHelper;

import cn.zdsoft.common.DirectoryUtil;
import cn.zdsoft.common.FileUtil;
import cn.zdsoft.common.GZipUtil;
import cn.zdsoft.common.PathUtil;
import cn.zdsoft.common.StringUtil;
import cn.zdsoft.common.model.DataTable;

/**
 * 文件扫描
 * @author 段江涛
 * @date 2018-06-27
 */
public class FileScan implements Runnable {
	private boolean isRunning = true;

	@Override
	public void run() {
		if (StringUtil.IsNullOrEmpty(SystemConfig.SourceDir)) {
			LogHelper.getLogger().warn("没有配置文件来源目录，不解析文件");
			return;
		}

		// 创建目录
		DirectoryUtil.CreateDir(SystemConfig.SourceDir);
		DirectoryUtil.CreateDir(SystemConfig.ErrorDir);
		DirectoryUtil.CreateDir(SystemConfig.ConvertLogDir);
		DirectoryUtil.CreateDir(SystemConfig.OutputDir);
		DirectoryUtil.CreateDir(SystemConfig.TempDir);

		while (isRunning) {
			try {
				File[] files = new File(SystemConfig.SourceDir).listFiles();
				for (File file : files) {
					if (FileUtil.isFileInUse(file)) {
						continue;
					}

					String extension = PathUtil.GetFileExtension(file.getName()).toLowerCase();
					if (!extension.equals(".gz") && !extension.equals(".gzip") && !extension.equals(".xml")) {
						continue;
					}

					List<Integer> list = new ArrayList<Integer>();
					for (ConvertFirm firm : SystemConfig.ConvertFirms) {
						for (ConvertTask task : firm.Tasks) {
							// 解析
							list.add(fileAction(file, firm, task));
						}
					}
					// 判断结果
					if (list.stream().filter(f -> f == 1).count() == list.size()) {
						LogHelper.getLogger().info("文件" + file.getAbsolutePath() + "未配置解析规则,直接删除");
						file.delete();
					} else if (list.stream().filter(f -> f == 3).count() > 0) {
						LogHelper.getLogger().error("文件" + file.getAbsolutePath() + "处理失败,移动到失败目录");
						String toFile = PathUtil.Combine(SystemConfig.ErrorDir, file.getName());
						MoveFile(file.getAbsolutePath(), toFile);
					}else{
						LogHelper.getLogger().error("文件" + file.getAbsolutePath() + "处理成功,直接删除文件");
						file.delete();
					}
				}
			} catch (Exception e) {
				LogHelper.getLogger().error("文件监听异常", e);
			}
		}

	}

	/**
	 * 正式解析文件
	 *  1-丢弃,不处理
	    2-处理成功
	    3-处理失败
	 * @param file
	 * @param firm
	 * @param task
	 * @return
	 * @throws IOException
	 */
	private int fileAction(File file, ConvertFirm firm, ConvertTask task) throws IOException {
		try {
			boolean con = false;
			for (String sp : task.SearchPatterns) {
				if (file.getName().contains(sp.replace("*", ""))) {
					con = true;// 需要解析
					break;
				}
			}

			if (con) {
				DataTable dt = ReadFile(file);
				String convertId=UUID.randomUUID().toString();
				new FileConvert(task, firm, convertId).DealFile(dt);
				return 1;
			} else {
				return 2;
			}
		} catch (Exception ex) {
			return 3;
		}
	}

	/**
	 * 把文件读取为DataTable
	 * @param file
	 * @return
	 * @throws IOException 
	 * @throws DocumentException 
	 */
	private DataTable ReadFile(File file) throws IOException, DocumentException {
		String content = "";
		DataTable dt = null;
		switch (PathUtil.GetFileExtension(file.getAbsolutePath())) {
		case ".gz":
			content = GZipUtil.UnCompress(file.getAbsolutePath());
			dt = StringUtil.String2Map(content);
			break;
		case ".xml":
			dt = Helper.Xml2Map(file);
			break;
		case ".gzip":
			content = GZipUtil.UnCompress(file.getAbsolutePath());
			dt = Helper.Xml2Map(content);
			break;
		default:
			dt = new DataTable();
			break;
		}
		return dt;
	}

	/**
	 * 将文件移动到指定目录
	 * @param fromFile
	 * @param toFile
	 * @throws IOException
	 */
	private void MoveFile(String fromFile, String toFile) throws IOException {
		FileUtil.CopyFile(fromFile, toFile);
		new File(fromFile).delete();
	}

	public void stop() {
		isRunning = false;
	}

}
