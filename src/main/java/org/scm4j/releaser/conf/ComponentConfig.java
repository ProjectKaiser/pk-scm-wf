package org.scm4j.releaser.conf;

import org.scm4j.commons.RegexConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ComponentConfig {

	private RegexConfig config;

	@SuppressWarnings("unchecked")
	public void loadFromUrlsAsYaml(List<String> yamlUrls) {
		Yaml yaml = new Yaml();
		URLContentLoader loader = new URLContentLoader();
		List<String> yamlContents;
		try {
			yamlContents = loader.getContentsFromUrls(yamlUrls);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		List<LinkedHashMap<Object, Object>> maps = new ArrayList<>();
		for (String yamlContent : yamlContents) {
			LinkedHashMap<Object, Object> map = (LinkedHashMap<Object, Object>) yaml.load(yamlContent);
			maps.add(map);
		}
		LinkedHashMap<Object, Object>[] mapsArray = maps.toArray(new LinkedHashMap[maps.size()]);
		config = new RegexConfig(mapsArray);
	}
}
