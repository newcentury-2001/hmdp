package org.javaup.core;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 隔离区域选择器
 * @author: 阿星不是程序员
 **/
public class IsolationRegionSelector {

	private final AtomicInteger count = new AtomicInteger(0);

	private final Integer thresholdValue;

	public IsolationRegionSelector(Integer thresholdValue) {
		this.thresholdValue = thresholdValue;
	}

	private int reset() {
		count.set(0);
		return count.get();
	}
	
	public synchronized int getIndex() {
		int cur = count.get();
		if (cur >= thresholdValue) {
			cur = reset();
		} else {
			count.incrementAndGet();
		}
		return cur;
	}
}
