package com.gb.apm.kafka.stream.apm.common.model.v3;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.gb.apm.kafka.stream.apm.common.model.utils.JacksonUtils;
import com.gb.apm.model.Annotation;

/**
 * @author xuelong.chen
 */
public class TreeStackFrame implements Serializable {

	private static final long serialVersionUID = 1999058604252210552L;
	
	/**
	 * 附带应用（链路）信息，来区分不同应用（链路）下的同一个方法
	 */
	private int chainMethodNodeKey;
	private String api;
	private Short sequence;
	private Short caller;
	private int applicationId;
	
	/**
	 * 用来区分是普通方法，RPC，还是sql查询，还是redis等
	 */
	private Short serviceType;
	
	/**
	 * 某些类型的方法节点可能需要相关的元数据来确定唯一
	 * 例如dubbo consumer节点，需要rpc元数据
	 * mysql_executequery节点，需要sql元数据
	 */
	private List<Annotation> annotations;
	
	@SuppressWarnings("unchecked")
	public <T> T findAnnotation(int key) {
		if(this.annotations == null || this.annotations.size() == 0) {
			return null;
		}else {
			for(Annotation annotation : annotations) {
				if(annotation.getKey() == key) {
					return (T) annotation.getValue();
				}
			}
		}
		return null;
	}
	
	public void addAnnotation(int key, String value) {
		Annotation annotation = new Annotation(key,value);
		addAnnotation(annotation);
	}
	
	public void addAnnotation(int key, int value) {
		Annotation annotation = new Annotation(key,value+"");
		addAnnotation(annotation);
	}
	
	public void addAnnotation(int key, Object value) {
		Annotation annotation = new Annotation(key,JacksonUtils.toJson(value));
		addAnnotation(annotation);
	}
	
	private void addAnnotation(Annotation annotation) {
		if(this.annotations == null) {
			this.annotations = new ArrayList<>();
		}
		this.annotations.add(annotation);
	}

	public String getApi() {
		return api;
	}

	public void setApi(String api) {
		this.api = api;
	}

	public Short getSequence() {
		return sequence;
	}

	public void setSequence(Short sequence) {
		this.sequence = sequence;
	}

	public Short getCaller() {
		return caller;
	}

	public void setCaller(Short caller) {
		this.caller = caller;
	}

	public int getChainMethodNodeKey() {
		return chainMethodNodeKey;
	}

	public void setChainMethodNodeKey(int chainMethodNodeKey) {
		this.chainMethodNodeKey = chainMethodNodeKey;
	}

	public Short getServiceType() {
		return serviceType;
	}

	public void setServiceType(Short serviceType) {
		this.serviceType = serviceType;
	}

	public List<Annotation> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(List<Annotation> annotations) {
		this.annotations = annotations;
	}
	
	public int getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(int applicationId) {
		this.applicationId = applicationId;
	}

	@Override
	public int hashCode() {
		int hashCode = this.api.hashCode();
		if(this.annotations != null) {
			for(Annotation annotation : annotations) {
				hashCode = hashCode * 31 + annotation.getValue().hashCode();
			}
		}
		return hashCode;
	}
}
