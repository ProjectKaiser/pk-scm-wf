import org.scm4j.vcs.api.*;
import org.scm4j.vcs.api.exceptions.EVCSBranchExists;
import org.scm4j.vcs.api.exceptions.EVCSBranchNotFound;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;
import org.scm4j.vcs.api.exceptions.EVCSTagExists;

import java.util.*;

public class TestVCS implements IVCS {

	private Map<String, Map<String, Map<String, String>>> branchCommits = new HashMap<>();
	private Map<String, VCSTag> tags = new HashMap<>();
	private Map<String, String> branchHeads = new HashMap<>();
	private String url;
	private Map<String, String> logMessages = new HashMap<>();

	public TestVCS(String url) {
		this.url = url;
	}

	@Override
	public void createBranch(String srcBranchName, String dstBranchName, String commitMessage) throws EVCSBranchExists {
		if (branchCommits.containsKey(dstBranchName)) {
			throw new EVCSBranchExists(dstBranchName);
		}
		branchCommits.put(dstBranchName, new HashMap<>());
	}

	@Override
	public VCSMergeResult merge(String srcBranchName, String dstBranchName, String commitMessage) {
		return null;
	}

	@Override
	public void deleteBranch(String branchName, String commitMessage) {
		branchCommits.remove(branchName);
	}

	@Override
	public void setCredentials(String user, String password) {

	}

	@Override
	public void setProxy(String host, int port, String proxyUser, String proxyPassword) {

	}

	@Override
	public String getRepoUrl() {
		return url;
	}

	@Override
	public String getFileContent(String branchName, String fileRelativePath, String revision) throws EVCSFileNotFound {
		Map<String, Map<String, String>> commits = this.branchCommits.get(branchName);
		if (commits == null) {
			throw new EVCSBranchNotFound(url, branchName);
		}
		Map<String, String> files = revision == null ? commits.get(branchHeads.get(branchName)) :
				commits.get(revision);
		String fileContent = files.get(fileRelativePath);
		if (fileContent == null) {
			throw new EVCSFileNotFound(url, branchName, fileRelativePath, revision);
		}

		return fileContent;
	}

	@Override
	public VCSCommit setFileContent(String branchName, String filePath, String content, String commitMessage) {
		Map<String, Map<String, String>> commits = this.branchCommits.get(branchName);
		if (commits == null) {
			throw new EVCSBranchNotFound(url, branchName);
		}

		String head = branchHeads.get(branchName);
		Map<String, String> files = commits.get(head);
		if (files == null) {
			files = new HashMap<>();
			commits.put(head, files);
		}
		files.put(filePath, content);
		String rev = getNewRevision();
		commits.put(rev, files);
		branchHeads.put(branchName, rev);
		logMessages.put(rev, commitMessage);
		return new VCSCommit(rev, commitMessage, null);
	}

	private String getNewRevision() {
		return UUID.randomUUID().toString();
	}

	@Override
	public List<VCSDiffEntry> getBranchesDiff(String srcBranchName, String destBranchName) {
		return null;
	}

	@Override
	public Set<String> getBranches(String path) {
		Set<String> res = new HashSet<>();
		for (String branch : branchCommits.keySet()) {
			if (branch.startsWith(path)) {
				res.add(branch);
			}
		}
		return res;
	}

	@Override
	public List<VCSCommit> log(String branchName, int limit) {
		List<VCSCommit> res = new ArrayList<>();
		Map<String, Map<String, String>> commits = branchCommits.get(branchName);
		if (commits == null) {
			return res;
		}
		for (String revision : commits.keySet()) {
			res.add(new VCSCommit(revision, logMessages.get(revision), null));
		}
		return res;
	}

	@Override
	public String getVCSTypeString() {
		return "test";
	}

	@Override
	public VCSCommit removeFile(String branchName, String filePath, String commitMessage) {
		return null;
	}

	@Override
	public List<VCSCommit> getCommitsRange(String branchName, String startRevision, String endRevision) {
		return null;
	}

	@Override
	public List<VCSCommit> getCommitsRange(String branchName, String startRevision, WalkDirection direction, int limit) {
		return null;
	}

	@Override
	public VCSCommit getHeadCommit(String branchName) {
		String revision = branchHeads.get(branchName);
		if (revision == null) {
			return null;
		}
		return new VCSCommit(revision, logMessages.get(revision), null);
	}

	@Override
	public Boolean fileExists(String branchName, String filePath) {
		return null;
	}

	@Override
	public VCSTag createTag(String branchName, String tagName, String tagMessage, String revisionToTag) throws EVCSTagExists {
		return null;
	}

	@Override
	public List<VCSTag> getTags() {
		return null;
	}

	@Override
	public void removeTag(String tagName) {

	}

	@Override
	public void checkout(String branchName, String targetPath, String revision) {

	}

	@Override
	public List<VCSTag> getTagsOnRevision(String revision) {
		return null;
	}
}
