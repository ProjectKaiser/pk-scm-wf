package org.scm4j.releaser.conf;

import org.apache.commons.io.FileUtils;
import org.scm4j.releaser.SCMReleaser;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DelayedTagsFile {
	
	public static final String MISSING_TO_STRING_MESSAGE = "<missing>";
	private final File delayedTagsFile;
	
	public DelayedTagsFile() {
		delayedTagsFile = new File(SCMReleaser.DELAYED_TAGS_FILE_NAME);
	}
	
	public String getRevisitonByUrl(String url) {
		if (!delayedTagsFile.exists()) {
			return null;
		}
		Map<String, String> delayedTags = getContent();
		return delayedTags.get(url);
	}
	
	protected String loadContent() throws IOException {
		return FileUtils.readFileToString(delayedTagsFile, StandardCharsets.UTF_8);
	}
	
	protected void saveContent(String content) throws IOException {
		FileUtils.writeStringToFile(delayedTagsFile, content, StandardCharsets.UTF_8);
	}

	public Map<String, String> getContent() {
		if (!delayedTagsFile.exists()) {
			return new HashMap<>();
		}
		Yaml yaml = new Yaml();
		try {
			@SuppressWarnings("unchecked")
			Map<String, String> delayedTags = yaml.loadAs(loadContent(), Map.class);
			if (delayedTags == null) {
				return new HashMap<>();
			}
			return delayedTags;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void writeUrlRevision(String url, String revision) throws IOException {
		if (!delayedTagsFile.exists()) {
			delayedTagsFile.createNewFile();
		}
		
		Map<String, String> content = getContent();
		String previousRevision = content.put(url, revision);
		if (previousRevision == null || !previousRevision.equals(revision)) {
			writeContent(content);
		}
	}
	
	public void delete() {
		delayedTagsFile.delete();
	}
	
	@Override
	public String toString() {
		if (!delayedTagsFile.exists()) {
			return MISSING_TO_STRING_MESSAGE;
		}
		return getContent().toString();
	}

	public void removeRevisionByUrl(String url) {
		Map<String, String> content = getContent();
		String removedRevision = content.remove(url);
		if (removedRevision != null) {
			writeContent(content);
		}
	}

	private void writeContent(Map<String, String> content) {
		Yaml yaml = new Yaml();
		try {
			saveContent(yaml.dumpAsMap(content));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
