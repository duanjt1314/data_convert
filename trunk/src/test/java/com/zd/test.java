package com.zd;

import java.util.Date;

import org.junit.Test;

import cn.zdsoft.common.DateUtil;

public class test {
	@Test
	public void testDate() {
		long a = 1543634542;
		long b = 1543720942;
		Date begin = new Date(a * 1000);// 小
		Date end = new Date(b * 1000);
		System.out.println(begin);
		System.out.println(end);
		System.out.println("--------------");
		System.out.println("begin:" + DateUtil.Format(begin, "yyyy-MM-dd HH:mm:ss"));
		System.out.println("end:" + DateUtil.Format(end, "yyyy-MM-dd HH:mm:ss"));

		System.out.println("--------------");
		
		for (Date i = begin; i.getTime()<=end.getTime(); i = DateUtil.AddDay(i, 1)) {
			System.out.println(DateUtil.Format(i, "yyyy-MM-dd HH:mm:ss"));
		}
		
		System.out.println("--------------");
		System.out.println(begin.equals(end));
		begin = DateUtil.AddDay(begin, 1);
		System.out.println("begin:" + DateUtil.Format(begin, "yyyy-MM-dd HH:mm:ss"));
		System.out.println(end.after(begin));
		System.out.println(begin.after(end));
		System.out.println(end.before(begin));
		System.out.println(begin.before(end));
		System.out.println(begin.equals(end));
		System.out.println(begin.getTime()<=end.getTime());
	}
}
