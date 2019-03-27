package com.gb.apm.web.controller;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gb.apm.web.timer.RolloverIndicesTimer;

@RestController
public class ToolController {
	
	@Autowired
	RolloverIndicesTimer rolloverApi;
	
	/**
	 * for test
	 * @return
	 */
	@RequestMapping("/tool/isactive")
	public Map<String, Object> isActive() {
		return Collections.singletonMap("result", "success");
	}

	/**
	 * for test
	 * @return
	 */
	@RequestMapping("/tool/rollover")
	public Map<String, Object> rollover() {
		this.rolloverApi.rollover();
		return Collections.singletonMap("result", "success");
	}
	
	/**
	 * for test
	 * @return
	 */
	@RequestMapping("/tool/expireIndices")
	public Map<String, Object> deleteExpireIndices(){
		this.rolloverApi.deleteExpiredIndices();
		return Collections.singletonMap("result", "success");
	}
	
	/**
	 * for test
	 * @return
	 */
	@RequestMapping("/tool/esinit")
	public Map<String, Object> esInit(){
		try {
			this.rolloverApi.afterPropertiesSet();
		} catch (Exception e) {
			return Collections.singletonMap("result", "fail");
		}
		return Collections.singletonMap("result", "success");
	}
	
	/**
	 * for test
	 * @return
	 */
	@RequestMapping("/tool/clean")
	public Map<String, Object> clean(){
		try {
			this.rolloverApi.cleanEs();
		} catch (Exception e) {
			return Collections.singletonMap("result", "fail");
		}
		return Collections.singletonMap("result", "success");
	}
	
	/**
	 * for test
	 * @return
	 */
	@RequestMapping("/tool/stop")
	public Map<String, Object> stop(){
		try {
			this.rolloverApi.stop();
		} catch (Exception e) {
			return Collections.singletonMap("result", "fail");
		}
		return Collections.singletonMap("result", "success");
	}
	
	/**
	 * for test
	 * @return
	 */
	@RequestMapping("/tool/start")
	public Map<String, Object> start(){
		try {
			this.rolloverApi.start();
		} catch (Exception e) {
			return Collections.singletonMap("result", "fail");
		}
		return Collections.singletonMap("result", "success");
	}
	
}
