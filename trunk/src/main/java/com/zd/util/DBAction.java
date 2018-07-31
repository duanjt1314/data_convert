package com.zd.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import org.springframework.jdbc.core.JdbcTemplate;
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
	 * 根据数据类型获取不同的数据库驱动
	 * @return
	 */
	private String getDBDriver() {
		switch (dataBase.getDbType()) {
		case "mysql":
			return "com.mysql.jdbc.Driver";
		case "oracle":
			return "oracle.jdbc.driver.OracleDriver";
		default:
			return "com.microsoft.sqlserver.jdbc.SQLServerDriver";// sqlserver
		}
	}

	/**
	 * 根据数据库类型获取不同的数据库连接地址
	 * @return
	 */
	private String getDBUrl() {
		switch (dataBase.getDbType()) {
		case "mysql":
			return "jdbc:mysql://" + dataBase.getIp() + ":" + dataBase.getPort() + "/" + dataBase.getServiceName();
		case "oracle":
			return "jdbc:oracle:thin:@" + dataBase.getIp() + ":" + dataBase.getPort() + ":" + dataBase.getServiceName();
		default:
			return "jdbc:sqlserver://" + dataBase.getIp() + ":" + dataBase.getPort() + ";DatabaseName=" + dataBase.getServiceName();// sqlserver
		}
	}

	/**
	 * 根据sql脚本查询数据
	 * @param sql
	 * @return
	 */
	public DataTable getData(String sql) {
		try {
			DataTable dt = new DataTable();
			Class.forName(getDBDriver());
			Connection con = DriverManager.getConnection(getDBUrl(), dataBase.getUserName(), dataBase.getPassword());
			Statement statement = con.createStatement();
			ResultSet rs = statement.executeQuery(sql);

			while (rs.next()) {
				DataRow row = new DataRow();
				ResultSetMetaData rsmd = rs.getMetaData();

				for (int i = 1; i <= rsmd.getColumnCount(); i++) {
					row.put(rsmd.getColumnName(i), rs.getString(i));
				}
				dt.add(row);
			}
			rs.close();
			con.close();
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
		dmds.setDriverClassName(this.getDBDriver());
		dmds.setUrl(this.getDBUrl());
		dmds.setUsername(dataBase.getUserName());
		dmds.setPassword(dataBase.getPassword());
		return new JdbcTemplate(dmds);
	}
}
