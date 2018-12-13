package com.zd.config;

import java.util.List;

/**
 * ES对象连接基础配置
 * @author 段江涛
 * @date 2018-12-13
 */
public class ElasticSearch {
	public class Host{
		private String ip;
		private int port;
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
		public int getPort() {
			return port;
		}
		/**
		 * 设置 port
		 * @param 
		 */
		public void setPort(int port) {
			this.port = port;
		}
		
		
	}
	
	public class Setting{
		private String key;
		private String val;
		/**
		 * @return 获取 key
		 */
		public String getKey() {
			return key;
		}
		/**
		 * 设置 key
		 * @param 
		 */
		public void setKey(String key) {
			this.key = key;
		}
		/**
		 * @return 获取 val
		 */
		public String getVal() {
			return val;
		}
		/**
		 * 设置 val
		 * @param 
		 */
		public void setVal(String val) {
			this.val = val;
		}
		
		
	}
	
	public Host newHost(){
		return new Host();
	}
	
	public Setting newSetting(){
		return new Setting();
	}
	
	private String user;
	private String password;
	private List<Host> hosts;
	private List<Setting> settings;
	/**
	 * @return 获取 user
	 */
	public String getUser() {
		return user;
	}
	/**
	 * 设置 user
	 * @param 
	 */
	public void setUser(String user) {
		this.user = user;
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
	/**
	 * @return 获取 hosts
	 */
	public List<Host> getHosts() {
		return hosts;
	}
	/**
	 * 设置 hosts
	 * @param 
	 */
	public void setHosts(List<Host> hosts) {
		this.hosts = hosts;
	}
	/**
	 * @return 获取 settings
	 */
	public List<Setting> getSettings() {
		return settings;
	}
	/**
	 * 设置 settings
	 * @param 
	 */
	public void setSettings(List<Setting> settings) {
		this.settings = settings;
	}
	
	
	
}
