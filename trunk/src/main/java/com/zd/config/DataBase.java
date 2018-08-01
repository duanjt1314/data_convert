package com.zd.config;

public class DataBase {
	private String url; // 服务器端口
	private String driver; // 数据库名/服务名
	private String userName; // 登录帐号
	private String password; // 登录密码
	/**
	 * @return 获取 url
	 */
	public String getUrl() {
		return url;
	}
	/**
	 * 设置 url
	 * @param 
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	/**
	 * @return 获取 driver
	 */
	public String getDriver() {
		return driver;
	}
	/**
	 * 设置 driver
	 * @param 
	 */
	public void setDriver(String driver) {
		this.driver = driver;
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
