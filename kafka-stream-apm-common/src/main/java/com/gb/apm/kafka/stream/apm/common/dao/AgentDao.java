package com.gb.apm.kafka.stream.apm.common.dao;

import java.util.List;

import com.gb.apm.kafka.stream.apm.common.model.v3.AgentInfo;

public interface AgentDao {
	public void addAgentInfo(AgentInfo agent) throws DaoException;
	public List<AgentInfo> getAgent() throws DaoException;
}
