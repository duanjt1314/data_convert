package com.zd.convert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.kafka.common.security.auth.KafkaPrincipal;

import com.zd.config.ConvertFirm;
import com.zd.config.ConvertTask;
import com.zd.config.SystemConfig;
import com.zd.kafka.DBProcess;
import com.zd.kafka.ESProcess;
import com.zd.kafka.FileScan;
import com.zd.kafka.KafKaProcess;
import com.zd.util.LogHelper;

import cn.zdsoft.common.StringUtil;

/**
 * 具体逻辑处理程序
 * 
 * @author Administrator
 *
 */
public class StartUp extends Thread {
	private FileScan fileScan = new FileScan();
	private List<DBProcess> dBPros = new ArrayList<DBProcess>();
	private List<ESProcess> esPros = new ArrayList<ESProcess>();
	private List<KafKaProcess> kaPros = new ArrayList<KafKaProcess>();
	
	@Override
	public void run() {
		new Thread(fileScan).start();

		// 数据库
		for (ConvertFirm firm : SystemConfig.ConvertFirms) {
			for (ConvertTask task : firm.Tasks) {
				if (task.DbAble) {
					DBProcess dbProcess = new DBProcess(task, firm);
					dbProcess.start();
					dBPros.add(dbProcess);
				} else if (task.EsAble) {
					ESProcess esProcess = new ESProcess(task, firm);
					esProcess.start();
					esPros.add(esProcess);
				}else if(!StringUtil.IsNullOrEmpty(task.Topic)){
					KafKaProcess kaProcess=new KafKaProcess(task, firm,SystemConfig.KafkaUrl);
					kaProcess.start();
					kaPros.add(kaProcess);
				}else{
					LogHelper.logger.error("任务:"+task.TaskId+",没有匹配到转换规则.");
				}
			}
		}
	}

	public void Start() {
		start();
	}

	public void Stop() {
		fileScan.stop();
		for (DBProcess dbProcess : dBPros) {
			dbProcess.finish();
		}
		for (ESProcess esProcess : esPros) {
			esProcess.interrupt();//停止
		}
		for (KafKaProcess kaProcess : kaPros) {
			kaProcess.finish();
		}
		LogHelper.getLogger().info("程序停止成功");
	}
}
