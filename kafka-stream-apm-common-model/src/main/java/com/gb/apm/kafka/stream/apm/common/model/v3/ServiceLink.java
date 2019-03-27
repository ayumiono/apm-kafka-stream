package com.gb.apm.kafka.stream.apm.common.model.v3;

import java.util.Set;

import com.gb.apm.kafka.stream.apm.common.annotation.MCollection;
import com.gb.apm.kafka.stream.apm.common.annotation.MID;

/**
 * 应用间依赖关系
 * 
 * 将关联关系保存在db
 * 
 * @author xuelong.chen
 *
 */
@MCollection("service_link")
public class ServiceLink {
	
	@MID
	private int sourceKey;
	private Set<Integer> targetKeys;//依据调用顺序排序
	private Service service;
	private boolean entrance;
	
	public ServiceLink() {}

	public ServiceLink(Service service, Set<Integer> targetKeys) {
		this.service = service;
		this.sourceKey = service.getId();
		this.targetKeys = targetKeys;
	}
	
	public ServiceLink(int key) {
		this.sourceKey = key;
	}

	public int getSourceKey() {
		return sourceKey;
	}

	public Set<Integer> getTargetKeys() {
		return targetKeys;
	}

	public Service getService() {
		return service;
	}

	public boolean isEntrance() {
		return entrance;
	}

	public void setEntrance(boolean entrance) {
		this.entrance = entrance;
	}
}
