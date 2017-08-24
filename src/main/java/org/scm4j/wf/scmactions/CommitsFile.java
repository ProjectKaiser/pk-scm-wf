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
	
	private File commitsFile;
	
	public CommitsFile() {
		commitsFile = new File(SCMWorkflow.COMMITS_FILE_NAME);
	}
	
	public String getRevisitonByComp(String compName) throws IOException {
		if (!commitsFile.exists()) {
			return null;
		}
		
		Yaml yaml = new Yaml();
		@SuppressWarnings("unchecked")
		Map<String, String> commits = yaml.loadAs(FileUtils.readFileToString(commitsFile, StandardCharsets.UTF_8), Map.class);
		if (commits == null) {
			return null;
		}
		return commits.get(compName);
	}
	
	public void writeCompRevision(String compName, String revision) throws IOException {
		if (!commitsFile.exists()) {
			commitsFile.createNewFile();
		}
		
		Yaml yaml = new Yaml();
		@SuppressWarnings("unchecked")
		Map<String, String> commits = yaml.loadAs(FileUtils.readFileToString(commitsFile, StandardCharsets.UTF_8), Map.class);
		if (commits == null) {
			commits = new HashMap<>();
		}
		commits.put(compName, revision);
		FileUtils.writeStringToFile(commitsFile, yaml.dump(commits), StandardCharsets.UTF_8);
	}
}
