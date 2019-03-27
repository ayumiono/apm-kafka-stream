package com.gb.apm.kafka.stream.apm.metrics.rest;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.gb.apm.kafka.stream.apm.common.dao.DaoException;
import com.gb.apm.kafka.stream.apm.common.dao.GBApmDao;
import com.gb.apm.kafka.stream.apm.common.model.StackFrame;
import com.gb.apm.kafka.stream.apm.common.model.StackFrameView;

@RestController
@RequestMapping("/trace")
public class MongoRestProxy {
	
	@Autowired
	private GBApmDao gbapmDao;
	
	@RequestMapping(value="/{trace_id}",method=RequestMethod.GET)
	public List<StackFrame> queryByTraceId(@PathVariable("trace_id") String traceId) {
		try {
			String _traceId = new String(Base64.getDecoder().decode(traceId),"UTF-8");
			return gbapmDao.querySpanByTraceId(_traceId);
		} catch (DaoException e) {
			return new ArrayList<>();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}
	
	@RequestMapping(value="/view/{trace_id}",method=RequestMethod.GET)
	public List<StackFrameView> queryByTraceId4View(@PathVariable("trace_id") String traceId) {
		try {
			String _traceId = new String(Base64.getDecoder().decode(traceId),"UTF-8");
			List<StackFrame> stacks = gbapmDao.querySpanByTraceId(_traceId);
			if(stacks.size() == 0) return null;
			//计算前端展示用百分比
			Collections.sort(stacks, new Comparator<StackFrame>() {
				@Override
				public int compare(StackFrame o1, StackFrame o2) {
					int order = o1.getOrder();
					int _order = o2.getOrder();
					return (int) (order - _order);
				}
			});
			StackFrame entrance = stacks.get(0);
			int totalSpent = (int) (entrance.getPopTimeStamp() - entrance.getPushTimeStamp());
			long beginStamp = entrance.getPushTimeStamp();
			List<StackFrameView> rs = new ArrayList<>();
			for(StackFrame stack : stacks) {
				long pushTime = stack.getPushTimeStamp();
				long popTime = stack.getPopTimeStamp();
				long _spent = popTime-pushTime;
				long interval = pushTime - beginStamp;
				int lPercent = new BigDecimal(interval).multiply(new BigDecimal(100)).divide(new BigDecimal(totalSpent),RoundingMode.HALF_UP).intValue();
				int percentage = new BigDecimal(_spent).multiply(new BigDecimal(100)).divide(new BigDecimal(totalSpent),RoundingMode.HALF_UP).intValue();
				StackFrameView view = StackFrameView.view(stack);
				view.setlPercent(lPercent);
				view.setPercentage(percentage);
				rs.add(view);
			}
			return rs;
		} catch (DaoException e) {
			e.printStackTrace();
			return new ArrayList<>();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}
	
	public static void main(String[] args) throws UnsupportedEncodingException {
		String t = new String(Base64.getEncoder().encode("购物车服务(192.168.26.38)_1512975084127_1513320817105_53".getBytes(Charset.forName("UTF-8"))),"utf-8");
		System.out.println(t);
	}
}
