package com.zd.convert;

import static org.junit.Assert.*;

import org.junit.Test;

import cn.zdsoft.common.model.DataRow;

public class DataFormatTest {

	@Test
	public void testFormat() {
		DataRow row = new DataRow();
		row.put("NETBAR_CODE", "41521525152152");
		String str = DataFormat.Format(null, row, "substring(row[NETBAR_CODE],7,8)");
		System.out.println(str);
	}

	@Test
	public void testGetIP() {
		String str = DataFormat.getIP(2067077380);
		System.out.println(str);
		long a = DataFormat.ipToLong("61.135.169.125");
		System.out.println(a);
	}

}
