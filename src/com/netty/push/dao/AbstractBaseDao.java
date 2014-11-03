package com.netty.push.dao;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * DAO抽象父類
 * 
 * @author maofw
 * 
 */
public abstract class AbstractBaseDao {
	@Resource
	protected JdbcTemplate nettyJdbcTemplate;
	@Resource
	protected SimpleJdbcTemplate nettySimpleJdbcTemplate;
	@Resource
	protected NamedParameterJdbcTemplate nettyNamedParameterJdbcTemplate;
	@Resource
	protected SimpleJdbcCall nettySimpleJdbcCall;

	public JdbcTemplate getJdbcTemplate() {
		return nettyJdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate nettyJdbcTemplate) {
		this.nettyJdbcTemplate = nettyJdbcTemplate;
	}

	protected Long getPrimaryKey(String sequence) {
		if (sequence == null) {
			return null;
		}
		return this.nettyJdbcTemplate.queryForLong("select " + sequence + ".nextval from dual");
	}
}
