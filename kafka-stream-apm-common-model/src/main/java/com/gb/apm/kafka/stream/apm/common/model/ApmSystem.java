package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;
import java.util.Date;

import com.gb.apm.kafka.stream.apm.common.annotation.MCollection;
import com.gb.apm.kafka.stream.apm.common.annotation.MID;

@MCollection("apm_system")
public class ApmSystem implements Serializable{
	private static final long serialVersionUID = -9120355879841947023L;
	
	@MID
	private int system_num_id;
	private int tenant_num_id;
	private int data_sign;
	private String system_name;
	private Date create_dtme;
	private Date last_updtme;
	private int create_user_id;
	private int last_update_user_id;
	private String datasource_name;
	public int getSystem_num_id() {
		return system_num_id;
	}
	public void setSystem_num_id(int system_num_id) {
		this.system_num_id = system_num_id;
	}
	public int getTenant_num_id() {
		return tenant_num_id;
	}
	public void setTenant_num_id(int tenant_num_id) {
		this.tenant_num_id = tenant_num_id;
	}
	public int getData_sign() {
		return data_sign;
	}
	public void setData_sign(int data_sign) {
		this.data_sign = data_sign;
	}
	public String getSystem_name() {
		return system_name;
	}
	public void setSystem_name(String system_name) {
		this.system_name = system_name;
	}
	public Date getCreate_dtme() {
		return create_dtme;
	}
	public void setCreate_dtme(Date create_dtme) {
		this.create_dtme = create_dtme;
	}
	public Date getLast_updtme() {
		return last_updtme;
	}
	public void setLast_updtme(Date last_updtme) {
		this.last_updtme = last_updtme;
	}
	public int getCreate_user_id() {
		return create_user_id;
	}
	public void setCreate_user_id(int create_user_id) {
		this.create_user_id = create_user_id;
	}
	public int getLast_update_user_id() {
		return last_update_user_id;
	}
	public void setLast_update_user_id(int last_update_user_id) {
		this.last_update_user_id = last_update_user_id;
	}
	public String getDatasource_name() {
		return datasource_name;
	}
	public void setDatasource_name(String datasource_name) {
		this.datasource_name = datasource_name;
	}
}
