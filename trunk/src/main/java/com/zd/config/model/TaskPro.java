package com.zd.config.model;

public class TaskPro {
	private String firmId;
	private String taskId;
	private int proValue;
	
	public TaskPro(){
	}
	
	public TaskPro(String firmId, String taskId, int proValue) {
		this.firmId = firmId;
		this.taskId = taskId;
		this.proValue = proValue;
	}
	
	/**
	 * @return 获取 firmId
	 */
	public String getFirmId() {
		return firmId;
	}
	/**
	 * 设置 firmId
	 * @param 
	 */
	public void setFirmId(String firmId) {
		this.firmId = firmId;
	}
	/**
	 * @return 获取 taskId
	 */
	public String getTaskId() {
		return taskId;
	}
	/**
	 * 设置 taskId
	 * @param 
	 */
	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}
	/**
	 * @return 获取 proValue
	 */
	public int getProValue() {
		return proValue;
	}
	/**
	 * 设置 proValue
	 * @param 
	 */
	public void setProValue(int proValue) {
		this.proValue = proValue;
	}
	
	
}
