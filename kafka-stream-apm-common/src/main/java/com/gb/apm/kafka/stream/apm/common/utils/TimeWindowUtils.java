package com.gb.apm.kafka.stream.apm.common.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.internals.TimeWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeWindowUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(TimeWindowUtils.class);
	
	@SuppressWarnings("unchecked")
	public static Long[] extractGrafanaTimeRange(Map<String, Object> requestPayLoad) throws ParseException {
		Map<String, String> range = (Map<String, String>) requestPayLoad.get("range");
		SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		df2.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date from = df2.parse(range.get("from").toString());
		Date to = df2.parse(range.get("to").toString());
		logger.info("grafana query timerange {} - {}",from,to);
		long _from = from.getTime();
		long _to = to.getTime();
		return new Long[] {_from,_to};
	}
	
	public static Long[] extractTimeRange(Map<String, Object> requestPayLoad) throws ParseException {
		return new Long[] {(long)requestPayLoad.get("range_start"),(long)requestPayLoad.get("range_end")};
	}
	
	public static Long[] extractRange(Map<String, Object> requestPayLoad) throws ParseException {
		Map<String, Long> range = (Map<String, Long>) requestPayLoad.get("range");
		return new Long[] {(long)range.get("range_start"),(long)range.get("range_end")};
	}
	
	public static Long[] extractFromTo(TimeWindows twindows,long _from,long _to) {
		Map<Long, TimeWindow> windows = twindows.windowsFor(_from);
		Map<Long, TimeWindow> _window = twindows.windowsFor(_to);
		
		long timeFrom = Long.MAX_VALUE;
        long timeTo = Long.MIN_VALUE;
		
		for (long windowStartMs : windows.keySet()) {
            timeFrom = windowStartMs < timeFrom ? windowStartMs : timeFrom;
            timeTo = windowStartMs > timeTo ? windowStartMs : timeTo;
        }
		
		for (long windowStartMs : _window.keySet()) {
            timeFrom = windowStartMs < timeFrom ? windowStartMs : timeFrom;
            timeTo = windowStartMs > timeTo ? windowStartMs : timeTo;
        }
		return new Long[] {timeFrom,timeTo};
	}
}
