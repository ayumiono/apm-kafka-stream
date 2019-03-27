package com.gb.apm.dao.mongo;

import java.util.List;

import com.gb.apm.kafka.stream.apm.common.dao.GBPersistenceException;
import com.gb.apm.kafka.stream.apm.common.dao.ServiceLinkDao;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceLink;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceLinkMetaData;

public class ServiceLinkDaoImpl extends BaseImpl<ServiceLink> implements ServiceLinkDao {

	protected ServiceLinkDaoImpl() throws Exception {
		super(ServiceLink.class);
	}

	@Override
	public void addServiceLink(ServiceLink link) throws GBPersistenceException {
		try {
			use(APM_DB).insert(link);
		} catch (Exception e) {
			throw new GBPersistenceException("添加service依赖关系失败", e);
		}
	}

	@Override
	public ServiceLinkMetaData findServiceLink(int key) throws GBPersistenceException {
		try {
			ServiceLink condition = new ServiceLink(key);
			List<ServiceLink> links = use(APM_DB).query(condition);
			ServiceLink l = links.get(0);
			ServiceLinkMetaData metadata = new ServiceLinkMetaData(l.getService());
			loopQuery(metadata, l);
			return metadata;
		} catch (Exception e) {
			throw new GBPersistenceException("查询service依赖关系失败", e);
		}

	}

	private void loopQuery(ServiceLinkMetaData metadata, ServiceLink currentLink) throws GBMongodbException {
		if (currentLink.getTargetKeys().size() == 0) {
			return;
		} else {
			for (Integer li : currentLink.getTargetKeys()) {
				ServiceLink condition = new ServiceLink(li);
				List<ServiceLink> rs = use(APM_DB).query(condition);
				ServiceLink f = rs.get(0);
				ServiceLinkMetaData current = new ServiceLinkMetaData(f.getService());
				metadata.addLinkMetaData(current);
				loopQuery(current, f);
			}
		}
	}

	@Override
	public ServiceLink findServiceLinks(int serviceId) {
		// TODO Auto-generated method stub
		return null;
	}
}
