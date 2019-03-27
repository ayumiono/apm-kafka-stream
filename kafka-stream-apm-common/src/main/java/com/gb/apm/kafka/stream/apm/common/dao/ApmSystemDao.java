package com.gb.apm.kafka.stream.apm.common.dao;

import java.util.List;

import com.gb.apm.kafka.stream.apm.common.model.ApmSystem;

public interface ApmSystemDao {
	public List<ApmSystem> getAll() throws DaoException;
}
