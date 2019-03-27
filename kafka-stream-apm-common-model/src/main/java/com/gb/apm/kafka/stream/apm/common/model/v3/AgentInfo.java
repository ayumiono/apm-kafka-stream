package com.gb.apm.kafka.stream.apm.common.model.v3;

import com.gb.apm.kafka.stream.apm.common.annotation.MCollection;
import com.gb.apm.kafka.stream.apm.common.annotation.MID;

@MCollection("apm_agent_info")
public class AgentInfo {
	@MID
	private String agentId;
	private String agentName;
	public String getAgentId() {
		return agentId;
	}
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}
	public String getAgentName() {
		return agentName;
	}
	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}
}
