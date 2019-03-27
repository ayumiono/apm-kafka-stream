package com.gb.apm.kafka.stream.apm.common.model.v3;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.gb.apm.common.trace.ServiceType;

/**
 * 应用标识符
 * USER://com.gb.apm.proxy.Invoker.invoke(com.gb.apm.Invocation invocation):72
 * DUBBO_PROVIDER://com.gb.apm.service.Test:method?group=1.0&version=1.0
 * SPRING_CLOUD://userControler/get
 * MYSQL://host:port?Database=xxxx
 * 
 * @author xuelong.chen
 *
 */
public final class ServiceURL implements Serializable {

	private static final long serialVersionUID = -615940159263336829L;

	private String protocol;

	private String path;

	private Map<String, String> parameters;

	private volatile transient String full;

	private volatile transient String identity;
	
	private volatile transient String string;

	private volatile transient String parameter;
	
	public ServiceURL() {}

	public ServiceURL(ServiceType serviceType, String path, String... parameters) {
		this.protocol = serviceType.getName();
		this.path = path;
		this.parameters = toStringMap(parameters);
	}
	
	public static Map<String, String> toStringMap(String... pairs) {
        Map<String, String> parameters = new HashMap<String, String>();
        if (pairs.length > 0) {
            if (pairs.length % 2 != 0) {
                throw new IllegalArgumentException("pairs must be even.");
            }
            for (int i = 0; i < pairs.length; i = i + 2) {
                parameters.put(pairs[i], pairs[i + 1]);
            }
        }
        return parameters;
    }
	
	public ServiceURL(String protocol,String path,Map<String, String> parameters) {
		this.protocol = protocol;
		this.path = path;
		this.parameters = parameters;
	}

	public void addParameter(String key, String param) {

	}
	
	public Map<String, String> getParameters() {
        return parameters;
    }
	
	public String getPath() {
        return path;
    }

	public String toFullString() {
        if (full != null) {
            return full;
        }
        return full = buildString(true);
    }
	
	public String toParameterString() {
        if (parameter != null) {
            return parameter;
        }
        return parameter = toParameterString(new String[0]);
    }
	
	public String toParameterString(String... parameters) {
        StringBuilder buf = new StringBuilder();
        buildParameters(buf, false, parameters);
        return buf.toString();
    }
	
	public String toIdentityString() {
        if (identity != null) {
            return identity;
        }
        return identity = buildString(false); // only return identity message, see the method "equals" and "hashCode"
    }

	private String buildString(boolean appendParameter, String... parameters) {
		StringBuilder buf = new StringBuilder();
		if (protocol != null && protocol.length() > 0) {
			buf.append(protocol);
			buf.append("://");
		}
		String path = getPath();
		if (path != null && path.length() > 0) {
			buf.append(path);
		}
		if (appendParameter) {
			buildParameters(buf, true, parameters);
		}
		return buf.toString();
	}
	
	private void buildParameters(StringBuilder buf, boolean concat, String[] parameters) {
        if (getParameters() != null && getParameters().size() > 0) {
            List<String> includes = (parameters == null || parameters.length == 0 ? null : Arrays.asList(parameters));
            boolean first = true;
            for (Map.Entry<String, String> entry : new TreeMap<String, String>(getParameters()).entrySet()) {
                if (entry.getKey() != null && entry.getKey().length() > 0
                        && (includes == null || includes.contains(entry.getKey()))) {
                    if (first) {
                        if (concat) {
                            buf.append("?");
                        }
                        first = false;
                    } else {
                        buf.append("&");
                    }
                    buf.append(entry.getKey());
                    buf.append("=");
                    buf.append(entry.getValue() == null ? "" : entry.getValue().trim());
                }
            }
        }
    }
	
	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	/**
     * Parse url string
     *
     * @param url URL string
     * @return URL instance
     * @see URL
     */
    public static ServiceURL valueOf(String url) {
        if (url == null || (url = url.trim()).length() == 0) {
            throw new IllegalArgumentException("url == null");
        }
        String protocol = null;
        String path = null;
        Map<String, String> parameters = null;
        int i = url.indexOf("?"); // seperator between body and parameters 
        if (i >= 0) {
            String[] parts = url.substring(i + 1).split("\\&");
            parameters = new HashMap<String, String>();
            for (String part : parts) {
                part = part.trim();
                if (part.length() > 0) {
                    int j = part.indexOf('=');
                    if (j >= 0) {
                        parameters.put(part.substring(0, j), part.substring(j + 1));
                    } else {
                        parameters.put(part, part);
                    }
                }
            }
            url = url.substring(0, i);
        }
        i = url.indexOf("://");
        if (i >= 0) {
            if (i == 0) throw new IllegalStateException("url missing protocol: \"" + url + "\"");
            protocol = url.substring(0, i);
            url = url.substring(i + 3);
        } else {
            // case: file:/path/to/file.txt
            i = url.indexOf(":/");
            if (i >= 0) {
                if (i == 0) throw new IllegalStateException("url missing protocol: \"" + url + "\"");
                protocol = url.substring(0, i);
                url = url.substring(i + 1);
            }
        }

        path = url;
        return new ServiceURL(protocol, path, parameters);
    }
    
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServiceURL other = (ServiceURL) obj;
		if (parameters == null) {
			if (other.parameters != null)
				return false;
		} else if (!parameters.equals(other.parameters))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (protocol == null) {
			if (other.protocol != null)
				return false;
		} else if (!protocol.equals(other.protocol))
			return false;
		return true;
	}
	
	public String toString() {
        if (string != null) {
            return string;
        }
        return string = buildString(true);
    }
	
	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
        return result;
    }
}
