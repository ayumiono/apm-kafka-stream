package com.gb.apm.kafka.stream.apm.common.dao;

import java.util.List;

import com.gb.apm.kafka.stream.apm.common.model.Chain;
import com.gb.apm.kafka.stream.apm.common.model.PageResult;

public interface ChainTreeDao {
	
	public void updateTree(Chain chain) throws DaoException;
	
	public String insertTree(Chain chain)throws DaoException;
	
	public Chain queryTreeById(long id)throws DaoException;
	
	public boolean exist(long id) throws DaoException;
	
	public List<Chain> queryTreeBySystemId(int systemId)throws DaoException;
	
	public List<Chain> getAll()throws DaoException;
	
	public PageResult<Chain> pageQuery(PageResult<Chain> p,Chain chain)throws DaoException;
	
	public List<Chain> get(Chain chain) throws DaoException;
}
