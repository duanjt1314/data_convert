package com.zd.convert;

import static org.junit.Assert.*;

import org.junit.Test;

import cn.zdsoft.common.model.DataRow;

public class DataFormatTest {

	@Test
	public void testFormat() {
		DataRow row=new DataRow();
		row.put("NETBAR_CODE", "41521525152152");
		String str= DataFormat.Format(null, row, "substring(row[NETBAR_CODE],7,1)");
		System.out.println(str);
	}

}
