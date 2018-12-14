package com.zd.kafka;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import com.google.gson.Gson;
import com.mysql.fabric.xmlrpc.base.Data;
import com.zd.config.ConvertElastic;
import com.zd.config.ConvertFirm;
import com.zd.config.ConvertTask;
import com.zd.config.ProcessTask;
import com.zd.config.SystemConfig;
import com.zd.config.model.TaskPro;
import com.zd.convert.FileConvert;
import com.zd.util.ESAction;
import com.zd.util.LogHelper;

import cn.zdsoft.common.DateUtil;
import cn.zdsoft.common.model.DataTable;

public class ESProcess extends Thread {
	private ConvertTask convertTask;
	private ConvertFirm firmInfo;
	private String convertId;
	private TransportClient client;

	public ESProcess(ConvertTask convertTask, ConvertFirm firmInfo) {
		this.convertTask = convertTask;
		this.firmInfo = firmInfo;
		this.convertId = UUID.randomUUID().toString();
		try {
			this.client = new ESAction(SystemConfig.Elastic).getClient();
		} catch (UnknownHostException e) {
			LogHelper.logger.error("ES初始化失败", e);
		}
	}

	@Override
	public void run() {
		// 加载进度
		int p = ProcessTask.getProValue(this.firmInfo.FirmId, this.convertTask.TaskId);
		if (p > 0) {
			this.convertTask.ConvertElastic.setMinTime(p);
		}
		LogHelper.logger.info("任务:" + convertTask.TaskId + " 加载进度:" + p);

		fromES();

		LogHelper.logger.info("任务:" + convertTask.TaskId + " 正常退出");
	}

	private void fromES() {
		List<IndexTypeMapping> indexs = getIndexType(this.convertTask.ConvertElastic);
		LogHelper.logger.info("任务:[" + convertTask.TaskId + "]需要查询的索引是:" + new Gson().toJson(indexs));
		for (IndexTypeMapping indexTypeMapping : indexs) {
			try {
				if (this.isInterrupted()) {
					break;
				}

				// 查询条件
				BoolQueryBuilder queryBuiler = QueryBuilders.boolQuery();
				queryBuiler.must(QueryBuilders.rangeQuery(this.convertTask.ConvertElastic.getKeyName())//
						.gte(this.convertTask.ConvertElastic.getMinTime())//
						.lte(this.convertTask.ConvertElastic.getMaxTime())//
				);
				
				// 排序
				SortBuilder sort = SortBuilders//
						.fieldSort(this.convertTask.ConvertElastic.getKeyName())//
						.order(SortOrder.ASC);

				SearchResponse searchResponse = client.prepareSearch(indexTypeMapping.getIndex())//
						.setTypes(indexTypeMapping.getType())//
						.setSize(5000)//
						.setQuery(queryBuiler)//
						.addSort(sort)//
						// 这个游标维持多长时间
						.setScroll(TimeValue.timeValueMinutes(8))//
						.execute().actionGet();
				search(searchResponse);// 执行查询
			} catch (Exception e) {
				LogHelper.logger.error("ES查询错误,任务:" + convertTask.TaskId + ",索引:" + indexTypeMapping.getIndex(), e);
			}

		}

	}

	private void search(SearchResponse searchResponse) {
		while (!this.isInterrupted()) {
			List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
			for (SearchHit hit : searchResponse.getHits()) {
				Map<String, Object> map = hit.getSource();
				list.add(map);
			}
			if (list.size() > 0) {
				LogHelper.logger.info("任务:" + convertTask.TaskId + " 查询到数据，即将解析，数据条数:" + list.size());

				DataTable table = new DataTable(list);
				new FileConvert(convertTask, firmInfo, convertId).DealFile(table, "");
				int res = Integer.parseInt(list.get(list.size() - 1)//
						.get(this.convertTask.ConvertElastic.getKeyName()).toString());

				ProcessTask.saveProValue(new TaskPro(this.firmInfo.FirmId, this.convertTask.TaskId, res));
			}
			searchResponse = client.prepareSearchScroll(searchResponse.getScrollId())//
					.setScroll(TimeValue.timeValueMinutes(8)).execute().actionGet();
			if (searchResponse.getHits().getHits().length == 0) {
				break;
			}
		}
	}

	/**
	 * 根据ES的转换配置，返回需要查询的所有索引和类型的集合
	 * 索引会涉及到分库的概念
	 * @param convertElastic
	 * @return
	 */
	private List<IndexTypeMapping> getIndexType(ConvertElastic convertElastic) {
		Date begin = new Date(convertElastic.getMinTime() * 1000);
		Date end = new Date(convertElastic.getMaxTime() * 1000);
		List<IndexTypeMapping> itps = new ArrayList<IndexTypeMapping>();
		switch (convertElastic.getSplitType()) {
		case "year":
			for (Date i = begin; dateStringToLong(i, "yyyy") <= dateStringToLong(end, "yyyy"); i = DateUtil.AddYear(i, 1)) {
				IndexTypeMapping itp = new IndexTypeMapping();
				itp.setIndex(convertElastic.getIndex() + "_" + DateUtil.Format(i, "yyyy"));
				itp.setType(convertElastic.getType());
				itps.add(itp);
			}
			break;
		case "month":
			for (Date i = begin; dateStringToLong(i, "yyyyMM") <= dateStringToLong(end, "yyyyMM"); i = DateUtil.AddMonth(i, 1)) {
				IndexTypeMapping itp = new IndexTypeMapping();
				itp.setIndex(convertElastic.getIndex() + "_" + DateUtil.Format(i, "yyyyMM"));
				itp.setType(convertElastic.getType());
				itps.add(itp);
			}
			break;
		case "day":
			for (Date i = begin; dateStringToLong(i, "yyyyMMdd") <= dateStringToLong(end, "yyyyMMdd"); i = DateUtil.AddDay(i, 1)) {
				IndexTypeMapping itp = new IndexTypeMapping();
				itp.setIndex(convertElastic.getIndex() + "_" + DateUtil.Format(i, "yyyyMMdd"));
				itp.setType(convertElastic.getType());
				itps.add(itp);
			}
			break;
		default:// none
			IndexTypeMapping itp = new IndexTypeMapping();
			itp.setIndex(convertElastic.getIndex());
			itp.setType(convertElastic.getType());
			itps.add(itp);
			break;
		}
		return itps;
	}

	private long dateStringToLong(Date date, String format) {
		return Long.parseLong(DateUtil.Format(date, format));
	}

	/**
	 * 索引、类型映射关系类
	 * @author 段江涛
	 * @date 2018-12-14
	 */
	private class IndexTypeMapping {
		private String index;// 索引
		private String type;// 类型

		/**
		 * @return 获取 index
		 */
		public String getIndex() {
			return index;
		}

		/**
		 * 设置 index
		 * @param 
		 */
		public void setIndex(String index) {
			this.index = index;
		}

		/**
		 * @return 获取 type
		 */
		public String getType() {
			return type;
		}

		/**
		 * 设置 type
		 * @param 
		 */
		public void setType(String type) {
			this.type = type;
		}

	}

}
