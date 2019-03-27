package com.gb.apm.kafka.stream.apm.common.model.v3;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gb.apm.common.trace.AnnotationKey;
import com.gb.apm.kafka.stream.apm.common.annotation.MCollection;
import com.gb.apm.kafka.stream.apm.common.annotation.MID;
import com.gb.apm.model.Annotation;
import com.gb.apm.model.TSpanEvent;
import com.gb.apm.plugins.dubbo.DubboConstants;
import com.gb.apm.plugins.mysql.MySqlConstants;

@MCollection("apm_chain_tree")
public class Chain implements Serializable {
	private static final long serialVersionUID = 2901762949515610476L;

	@MID
	private int applicationKey;

	private List<TreeStackFrame> stacks;

	private Date createTime;
	private Date updateTime;
	private String applicationName;
	private int serviceTypeCode;

	public int getApplicationKey() {
		return applicationKey;
	}

	public void setApplicationKey(int applicationKey) {
		this.applicationKey = applicationKey;
	}

	public List<TreeStackFrame> getStacks() {
		return stacks;
	}

	public void setStacks(List<TreeStackFrame> stacks) {
		this.stacks = stacks;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public int getServiceTypeCode() {
		return serviceTypeCode;
	}

	public void setServiceTypeCode(int serviceTypeCode) {
		this.serviceTypeCode = serviceTypeCode;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	public static int getHashCode(String api, List<Annotation> annotations, short serviceTypeCode) {
		int hashCode = api.hashCode();
		if(serviceTypeCode == DubboConstants.DUBBO_CONSUMER_SERVICE_TYPE.getCode()) {
			Object annotationValue = findAnnotation(DubboConstants.DUBBO_INTERFACE_ANNOTATION_KEY.getCode(), annotations);
			hashCode = hashCode * 31 + annotationValue.hashCode();
		}else if(serviceTypeCode == MySqlConstants.MYSQL_EXECUTE_QUERY.getCode()) {
			Object annotationValue = findAnnotation(AnnotationKey.SQL.getCode(), annotations);
			hashCode = hashCode * 31 + annotationValue.hashCode();
		}
		return hashCode;
	}
	
	private static Object findAnnotation(int key,List<Annotation> annotations) {
		if(annotations == null || annotations.size() == 0) throw new RuntimeException("[TreeStackFrame.findAnnotation] annotation is null");
		for(Annotation annotation : annotations) {
			if(annotation.getKey() == key) {
				return annotation.getValue();
			}
		}
		throw new RuntimeException("[TreeStackFrame.findAnnotation] cannot match an annotation by" + key);
	}
	
	public static Chain generateChain(List<TSpanEvent> spans, int applicationKey,String applicationName,int serviceTypeCode) {
		Chain chain = new Chain();
		chain.setApplicationName(applicationName);
		chain.setApplicationKey(applicationKey);
		chain.setCreateTime(new Date());
		chain.setUpdateTime(new Date());
		chain.setServiceTypeCode(serviceTypeCode);
		Map<String, TreeStackFrame> tmp = new HashMap<>();//去重
		for(TSpanEvent span : spans) {
			String api = span.findAnnotation(AnnotationKey.API.getCode());
			Short sequence = span.getSequence();
			Short caller = span.getParentSequence();
			TreeStackFrame frame = new TreeStackFrame();
			frame.setApi(api);
			frame.setServiceType(span.getServiceType());
			frame.setCaller(caller);
			frame.setSequence(sequence);
			frame.setApplicationId(applicationKey);
			if(span.getServiceType() == DubboConstants.DUBBO_CONSUMER_SERVICE_TYPE.getCode()) {
				frame.addAnnotation(DubboConstants.DUBBO_INTERFACE_ANNOTATION_KEY.getCode(), frame.findAnnotation(DubboConstants.DUBBO_INTERFACE_ANNOTATION_KEY.getCode()));
			}else if(span.getServiceType() == MySqlConstants.MYSQL_EXECUTE_QUERY.getCode()) {
				frame.addAnnotation(AnnotationKey.SQL.getCode(), frame.findAnnotation(AnnotationKey.SQL.getCode()));
				//记录sql来源
				int parentSequence = span.getParentSequence();
				for(TSpanEvent s : spans) {
					if(s.getSequence() == parentSequence) {
						frame.addAnnotation(MySqlConstants.SQL_SOURCE.getCode(), s.findAnnotation(AnnotationKey.API.getCode()));
						break;
					}
				}
			}
			frame.setChainMethodNodeKey(applicationKey * 31 + frame.hashCode());//不同的chain下，相同的frame具有不同的chainMethodNodeKey
			tmp.put(api, frame);
		}
		chain.setStacks(Arrays.asList(tmp.values().toArray(new TreeStackFrame[] {})));
		return chain;
	}
	
	/**
	 * 提取出该应用（链路）下所有的sql，并附带上chainMethodNodeKey信息，以用来查看实时统计数据
	 * @return
	 */
	public List<Object[]> extractSqls() {
		List<Object[]> sqls = new ArrayList<>();
		for(TreeStackFrame stack : stacks) {
			if(stack.getServiceType() == MySqlConstants.MYSQL_EXECUTE_QUERY.getCode()) {
				String sql = stack.findAnnotation(AnnotationKey.SQL.getCode());
				sqls.add(new Object[] {sql,stack.getChainMethodNodeKey()});
			}
		}
		return sqls;
	}
	
	/**
	 * 提取出该应用（链路）下所有的rpc调用，并附带上chainMethodNodeKey信息，以用来查看实时统计数据
	 * @return
	 */
	public List<Object[]> extractRpcs(){
		List<Object[]> rpcs = new ArrayList<>();
		for(TreeStackFrame stack : stacks) {
			if(stack.getServiceType() == DubboConstants.DUBBO_CONSUMER_SERVICE_TYPE.getCode()) {
				String rpc = stack.findAnnotation(DubboConstants.DUBBO_INTERFACE_ANNOTATION_KEY.getCode());
				rpcs.add(new Object[] {rpc,stack.getChainMethodNodeKey()});
			}
		}
		return rpcs;
	}
}
