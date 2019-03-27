package com.gb.apm.kafka.stream.apm.common.model.v3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 分两层，外层为系统级别(ccash,cinventory,ccart...)connections
 * 内层为指定某个系统中的所有服务级别的connections
 * 
 * @author xuelong.chen
 *
 */
public class VizceralDataStruct {
	private String renderer;
	private String name;
	private long serverUpdateTime;
	private List<VizceralDataStruct> nodes;
	private List<Connection> connections;

	public static class Connection {
		private String source;// The source node of the connection, will log a warning if the node does not
								// exist.
		private String target;// The target node of the connection, will log a warning if the node does not
								// exist.
		private Map<String, Double> metrics;// These are the three default types/colors(normal/danger/warning) available
											// in the component and the colors that are used for the nodes themselves.
											// You are welcme to add others, or use other names instead knowing tha they
											// may not match the UI coloring appropriately.
		private List<Notice> notices;
		private Map<String, Double> metadata;// OPTIONAL Any data that may be handled by a plugin or other data that
												// isn't important to vizceral itself (if you want to show stuff on the
												// page that contains vizceral, for example). Since it is completely
												// optional and not handled by vizceral, you technically could use any
												// index, but this is the convention we use.

		public String getSource() {
			return source;
		}

		public void setSource(String source) {
			this.source = source;
		}

		public String getTarget() {
			return target;
		}

		public void setTarget(String target) {
			this.target = target;
		}

		public Map<String, Double> getMetrics() {
			return metrics;
		}

		public void setMetrics(Map<String, Double> metrics) {
			this.metrics = metrics;
		}

		public List<Notice> getNotices() {
			return notices;
		}

		public void setNotices(List<Notice> notices) {
			this.notices = notices;
		}

		public Map<String, Double> getMetadata() {
			return metadata;
		}

		public void setMetadata(Map<String, Double> metadata) {
			this.metadata = metadata;
		}
	}

	/**
	 * OPTIONAL Any notices that you want to show up in the sidebar
	 * 
	 * @author xuelong.chen
	 */
	public static class Notice {
		private String title;
		private String link;
		private int severity;// OPTIONAL 0(default) for info level, 1 for warning level, 2 for error level
								// (applies CSS styling)

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getLink() {
			return link;
		}

		public void setLink(String link) {
			this.link = link;
		}

		public int getSeverity() {
			return severity;
		}

		public void setSeverity(int severity) {
			this.severity = severity;
		}
	}

	public String getRenderer() {
		return renderer;
	}

	public void setRenderer(String renderer) {
		this.renderer = renderer;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getServerUpdateTime() {
		return serverUpdateTime;
	}

	public void setServerUpdateTime(long serverUpdateTime) {
		this.serverUpdateTime = serverUpdateTime;
	}

	public List<VizceralDataStruct> getNodes() {
		return nodes;
	}

	public void setNodes(List<VizceralDataStruct> nodes) {
		this.nodes = nodes;
	}
	
	public List<Connection> getConnections() {
		return connections;
	}

	public void setConnections(List<Connection> connections) {
		this.connections = connections;
	}

	public static VizceralDataStruct generateTestData() {
		VizceralDataStruct result = new VizceralDataStruct();
		result.setRenderer("global");
		result.setName("edge");
		result.setServerUpdateTime(System.currentTimeMillis());
		List<VizceralDataStruct> nodes = new ArrayList<>();
		result.setNodes(nodes);
		List<Connection> connections = new ArrayList<>();
		result.setConnections(connections);
		String nodeNamePrefix = "outer-";
		String innerNodeNamePrefix = "inner-";
		for (int i = 0; i < 3; i++) {
			VizceralDataStruct outer = new VizceralDataStruct();
			nodes.add(outer);
			String name = nodeNamePrefix + i;
			outer.setName(name);
			outer.setRenderer("region");
		}
		
		return result;
	}
	
	private static int randInt(int n,int m) {
		int w = m-n;
		return (int) Math.ceil(Math.random()*w + n);
	}
}
