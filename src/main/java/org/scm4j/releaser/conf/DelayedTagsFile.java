package org.scm4j.releaser.conf;

import org.apache.commons.io.FileUtils;
import org.scm4j.commons.Version;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DelayedTagsFile {
	
	public static final String MISSING_TO_STRING_MESSAGE = "<missing>";
	public static final String DELAYED_TAGS_FILE_NAME = "delayed-tags.yml";
	private final File delayedTagsFile;
	
	public DelayedTagsFile() {
		delayedTagsFile = new File(DELAYED_TAGS_FILE_NAME);
	}
	
	public DelayedTag getDelayedTagByUrl(String url) {
		if (!delayedTagsFile.exists()) {
			return null;
		}
		Map<String, DelayedTag> delayedTags = getContent();
		return delayedTags.get(url);
	}
	
	String loadContent() throws IOException {
		return FileUtils.readFileToString(delayedTagsFile, StandardCharsets.UTF_8);
	}
	
	void saveContent(String content) throws IOException {
		FileUtils.writeStringToFile(delayedTagsFile, content, StandardCharsets.UTF_8);
	}

	public Map<String, DelayedTag> getContent() {
		if (!delayedTagsFile.exists()) {
			return new HashMap<>();
		}
		Yaml yaml = new Yaml();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Map<String, String>> delayedTags = yaml.loadAs(loadContent(), Map.class);
			if (delayedTags == null) {
				return new HashMap<>();
			}
			return stringsToDelayedTagsMap(delayedTags);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<String, Map<String, String>> delayedTagsMapToStrings(Map<String, DelayedTag> delayedTags) {
		HashMap<String, Map<String, String>> res = new HashMap<>();
		for (Map.Entry<String, DelayedTag> entry : delayedTags.entrySet()) {
			HashMap<String, String> map = new HashMap<>();
			map.put("revision", entry.getValue().getRevision());
			map.put("version", entry.getValue().getVersion().toString());
			res.put(entry.getKey(), map);
		}
		return res;
	}

	private Map<String, DelayedTag> stringsToDelayedTagsMap(Map<String, Map<String, String>> delayedTags) {
		HashMap<String, DelayedTag> res = new HashMap<>();
		for (Map.Entry<String, Map<String, String>> entry : delayedTags.entrySet()) {
			DelayedTag tag = new DelayedTag(new Version(entry.getValue().get("version")), entry.getValue().get("revision"));
			res.put(entry.getKey(),tag);
		}
		return res;
	}

	public void writeUrlDelayedTag(String url, Version version, String revision) throws IOException {
		if (!delayedTagsFile.exists()) {
			delayedTagsFile.createNewFile();
		}
		
		Map<String, DelayedTag> content = getContent();
		DelayedTag tag = new DelayedTag(version, revision);
		DelayedTag previousTag = content.put(url, tag);
		if (previousTag == null || !previousTag.equals(tag)) {
			writeContent(content);
		}
	}
	
	public boolean delete() {
		return delayedTagsFile.delete();
	}
	
	@Override
	public String toString() {
		if (!delayedTagsFile.exists()) {
			return MISSING_TO_STRING_MESSAGE;
		}
		return getContent().toString();
	}

	public void removeTagByUrl(String url) {
		Map<String, DelayedTag> content = getContent();
		DelayedTag removedTag = content.remove(url);
		if (removedTag != null) {
			writeContent(content);
		}
	}

	private void writeContent(Map<String, DelayedTag> content) {
		Yaml yaml = new Yaml();
		try {
			Map<String, Map<String, String>> mappedContent = delayedTagsMapToStrings(content);
			saveContent(yaml.dumpAsMap(mappedContent));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
