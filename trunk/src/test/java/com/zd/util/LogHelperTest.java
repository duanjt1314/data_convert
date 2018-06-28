package com.zd.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class LogHelperTest {

	@Test
	public void test() {
		LogHelper.getLogger().info("OK");
	}

}
