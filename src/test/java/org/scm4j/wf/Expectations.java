package org.scm4j.wf;

import java.util.HashMap;
import java.util.Map;

public class Expectations {
	
	private final Map<String, Map<String, Object>> props = new HashMap<>();
	
	public Map<String, Map<String, Object>> getProps() {
		return props;
	}

	private Map<String, Object> getByName(String name) {
		Map<String, Object> res = props.get(name);
		if (res == null) {
			res = new HashMap<>();
			props.put(name, res);
		}
		return res;
	}
	
	public void put(String name, Class<?> clazz) {
		put(name, "class", clazz);
	}
	
	public void put(String name, String propName, Object propValue) {
		Map<String, Object> props = getByName(name);
		props.put(propName, propValue);
	}

}
