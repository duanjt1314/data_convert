package com.zd.config;

public class DataBase {
	private String dbType; // 数据库类型 mysql/oracle/sqlserver
	private String ip; // ip地址
	private String port; // 服务器端口
	private String serviceName; // 数据库名/服务名
	private String userName; // 登录帐号
	private String password; // 登录密码
	/**
	 * @return 获取 dbType
	 */
	public String getDbType() {
		return dbType;
	}
	/**
	 * 设置 dbType
	 * @param 
	 */
	public void setDbType(String dbType) {
		this.dbType = dbType;
	}
	/**
	 * @return 获取 ip
	 */
	public String getIp() {
		return ip;
	}
	/**
	 * 设置 ip
	 * @param 
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}
	/**
	 * @return 获取 port
	 */
	public String getPort() {
		return port;
	}
	/**
	 * 设置 port
	 * @param 
	 */
	public void setPort(String port) {
		this.port = port;
	}
	/**
	 * @return 获取 serviceName
	 */
	public String getServiceName() {
		return serviceName;
	}
	/**
	 * 设置 serviceName
	 * @param 
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	/**
	 * @return 获取 userName
	 */
	public String getUserName() {
		return userName;
	}
	/**
	 * 设置 userName
	 * @param 
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}
	/**
	 * @return 获取 password
	 */
	public String getPassword() {
		return password;
	}
	/**
	 * 设置 password
	 * @param 
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	
	

}
