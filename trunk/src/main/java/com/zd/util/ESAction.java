package com.zd.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;

import com.zd.config.ElasticSearch;
import com.zd.config.ElasticSearch.Host;
import com.zd.config.ElasticSearch.Setting;

import cn.zdsoft.common.StringUtil;

public class ESAction {
	ElasticSearch elasticSearch = null;

	public ESAction(ElasticSearch elasticSearch) {
		this.elasticSearch = elasticSearch;
	}

	/**
	 * 获得ES的连接
	 * @return
	 * @throws UnknownHostException
	 */
	public TransportClient getClient() throws UnknownHostException {
		// 设置
		org.elasticsearch.common.settings.Settings.Builder builder = Settings.builder();
		for (Setting setting : elasticSearch.getSettings()) {
			builder.put(setting.getKey(), setting.getVal());
		}
		builder.put("xpack.security.user", elasticSearch.getUser() + ":" + elasticSearch.getPassword());
		Settings setting = builder.build();// 构造settings

		TransportClient client = new PreBuiltXPackTransportClient(setting);

		for (Host host : elasticSearch.getHosts()) {
			InetSocketTransportAddress address = //
					new InetSocketTransportAddress(InetAddress.getByName(host.getIp()), host.getPort());
			client.addTransportAddress(address);
		}
		return client;
	}
}
