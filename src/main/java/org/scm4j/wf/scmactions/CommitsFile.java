package org.scm4j.wf.scmactions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.scm4j.wf.SCMWorkflow;
import org.yaml.snakeyaml.Yaml;

public class CommitsFile {
	
	private final File commitsFile;
	
	public CommitsFile() {
		commitsFile = new File(SCMWorkflow.COMMITS_FILE_NAME);
	}
	
	public String getRevisitonByComp(String compName) {
		if (!commitsFile.exists()) {
			return null;
		}
		Map<String, String> commits = getContent();
		return commits.get(compName);
	}

	public Map<String, String> getContent() {
		if (!commitsFile.exists()) {
			return new HashMap<>();
		}
		Yaml yaml = new Yaml();
		try {
			@SuppressWarnings("unchecked")
			Map<String, String> commits = yaml.loadAs(FileUtils.readFileToString(commitsFile, StandardCharsets.UTF_8), Map.class);
			if (commits == null) {
				return new HashMap<>();
			}
			return commits;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void writeCompRevision(String compName, String revision) throws IOException {
		if (!commitsFile.exists()) {
			commitsFile.createNewFile();
		}
		
		Map<String, String> content = getContent();
		String previousRevision = content.put(compName, revision);
		if (previousRevision != revision) {
			writeContent(content);
		}
	}
	
	public void delete() {
		commitsFile.delete();
	}
	
	@Override
	public String toString() {
		if (!commitsFile.exists()) {
			return "<missing>";
		}
		return getContent().toString();
	}

	public void removeRevisionsByComp(String compName) {
		Map<String, String> content = getContent();
		String removedRevision = content.remove(compName);
		if (removedRevision != null) {
			writeContent(content);
		}
	}

	private void writeContent(Map<String, String> content) {
		Yaml yaml = new Yaml();
		try {
			FileUtils.writeStringToFile(commitsFile, yaml.dump(content), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
