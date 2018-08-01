package com.zd.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.zd.config.DataBase;

import cn.zdsoft.common.model.DataRow;
import cn.zdsoft.common.model.DataTable;

public class DBAction {
	DataBase dataBase = null;

	public DBAction(DataBase dataBase) {
		this.dataBase = dataBase;
	}

	/**
	 * 根据sql脚本查询数据
	 * @param sql
	 * @return
	 */
	public DataTable getData(String sql) {
		try {
			DataTable dt=new DataTable();
			this.getJdbcTemplate().query(sql, new RowCallbackHandler(){

				@Override
				public void processRow(ResultSet rs) throws SQLException {
					DataRow row = new DataRow();
					ResultSetMetaData rsmd = rs.getMetaData();

					for (int i = 1; i <= rsmd.getColumnCount(); i++) {
						row.put(rsmd.getColumnName(i), rs.getString(i));
					}
					dt.add(row);
				}
				
			});
			
			return dt;
		} catch (Exception ex) {
			LogHelper.getLogger().error("数据库访问失败,sql:" + sql, ex);
			return new DataTable();
		}
	}
	
	/**
	 * 获取JdbcTemplate用于访问数据库
	 * @return
	 */
	public JdbcTemplate getJdbcTemplate(){
		DriverManagerDataSource dmds=new DriverManagerDataSource();
		dmds.setDriverClassName(dataBase.getDriver());
		dmds.setUrl(dataBase.getUrl());
		dmds.setUsername(dataBase.getUserName());
		dmds.setPassword(dataBase.getPassword());
		return new JdbcTemplate(dmds);
	}
}
