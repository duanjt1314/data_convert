package com.zd.kafka;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.zd.config.ConvertFirm;
import com.zd.config.ConvertTask;
import com.zd.config.ProcessTask;
import com.zd.config.SystemConfig;
import com.zd.config.model.TaskPro;
import com.zd.convert.FileConvert;
import com.zd.util.DBAction;
import com.zd.util.Helper;
import com.zd.util.LogHelper;
import com.zd.util.LogHelperTest;

import cn.zdsoft.common.model.DataRow;
import cn.zdsoft.common.model.DataTable;

/**
 * 数据库访问
 * @author 段江涛
 * @date 2018-07-31
 */
public class DBProcess extends Thread {
	private ConvertTask convertTask;
	private ConvertFirm firmInfo;
	private String convertId;
	private DBAction dbAction;
	private boolean started = false;

	public DBProcess(ConvertTask convertTask, ConvertFirm firmInfo) {
		this.convertTask = convertTask;
		this.firmInfo = firmInfo;
		this.convertId = UUID.randomUUID().toString();
		dbAction = new DBAction(SystemConfig.DataBase);
	}

	@Override
	public void run() {
		started = true;
		boolean first = true;
		int time = 0;
		int inttime = 0;

		while (started) {
			try {
				if (first) {
					int p = ProcessTask.getProValue(this.firmInfo.FirmId, this.convertTask.TaskId);
					if (p == 0)
						inttime = convertTask.ConvertSql.Start;
					else
						inttime = p;
					
					first = false;
					LogHelper.getLogger().debug("任务：" + convertTask.TaskId + " 初次访问，@@inttime设置为start的值");
				} else {
					if (convertTask.ConvertSql.ResetIntervalMinute > 0//
							&& time > convertTask.ConvertSql.ResetIntervalMinute * 60) {
						time = 0;
						inttime = 0;// 重置
						LogHelper.getLogger().debug("任务：" + convertTask.TaskId + " 到达了重置时间，@@inttime重置为0");
					}
				}

				String sql = convertTask.ConvertSql.Sql.replaceAll("@@inttime", inttime + "");// sql
				int res = executeDb(sql);
				if (res > 0) {
					inttime = res;
					ProcessTask.saveProValue(new TaskPro(this.firmInfo.FirmId, this.convertTask.TaskId, res));
				}

				time += 10;
				wait(10);// 等待10秒
			} catch (Exception e) {
				LogHelper.getLogger().error("任务:" + convertTask.TaskId + " 出现无法识别的异常", e);
			}
		}

	}

	/**
	 * 根据sql处理数据并返回当前最大的inttime
	 * @param sql
	 * @return
	 */
	private int executeDb(String sql) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("maxTime", 0);

		JdbcTemplate jdbcTemplate = dbAction.getJdbcTemplate();
		DataTable table = new DataTable();
		jdbcTemplate.query(sql, new RowCallbackHandler() {

			@Override
			public void processRow(ResultSet rs) throws SQLException {
				DataRow row = new DataRow();
				ResultSetMetaData rsmd = rs.getMetaData();

				for (int i = 1; i <= rsmd.getColumnCount(); i++) {
					row.put(rsmd.getColumnName(i), rs.getString(i));
				}
				table.add(row);
				map.put("maxTime", rs.getInt(convertTask.ConvertSql.KeyName));

				if (table.size() >= 5000) {
					new FileConvert(convertTask, firmInfo, convertId).DealFile(table, "");
					table.clear();
				}
			}

		});

		if (table.size() > 0) {
			new FileConvert(convertTask, firmInfo, convertId).DealFile(table, "");
		}

		return map.get("maxTime");
	}

	private void wait(int second) throws InterruptedException {
		int i = 0;
		while (i < second && started) {
			Thread.sleep(1000);
			i++;
		}
	}

	public void finish() {
		started = false;
	}

}
