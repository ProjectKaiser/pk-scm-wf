package org.scm4j.wf;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.scm4j.vcs.GitVCS;
import org.scm4j.vcs.GitVCSUtils;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.workingcopy.IVCSRepositoryWorkspace;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.api.workingcopy.VCSWorkspace;
import org.scm4j.wf.conf.EnvVarsConfigSource;
import org.scm4j.wf.conf.IConfigSource;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.conf.Version;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

public class TestEnvironment implements AutoCloseable {
	public static final String TEST_REPOS_FILE_NAME = "repos";
	public static final String TEST_CREDENTIALS_FILE_NAME = "credentials";
	public static final String TEST_ENVIRONMENT_DIR = new File(System.getProperty("java.io.tmpdir"), "scm4j-wf-test").getPath();
	public static final String TEST_ENVIRONMENT_URL = "file://localhost/" + TEST_ENVIRONMENT_DIR.replace("\\", "/");
	public static final String TEST_REMOTE_REPO_DIR = new File(TEST_ENVIRONMENT_DIR, "remote-repos").getPath();
	public static final String TEST_FEATURE_FILE_NAME = "feature.txt";
	public static final String TEST_DUMMY_FILE_NAME = "dummy.txt";
	
	public static final String PRODUCT_UNTILL = "eu.untill:unTill";
	public static final String PRODUCT_UBL = "eu.untill:UBL";
	public static final String PRODUCT_UNTILLDB = "eu.untill:unTillDb";
	
	public final String RANDOM_VCS_NAME_SUFFIX;

	private IVCS unTillVCS;
	private IVCS ublVCS;
	private IVCS unTillDbVCS;
	private File credsFile;
	private File reposFile;
	private final Version unTillVer = new Version("1.123.3-SNAPSHOT");
	private final Version ublVer = new Version("1.18.5-SNAPSHOT");
	private final Version unTillDbVer = new Version("2.59.1-SNAPSHOT");
	private File envDir;
	
	public TestEnvironment() {
		RANDOM_VCS_NAME_SUFFIX = UUID.randomUUID().toString();
	}

	public void generateTestEnvironment() throws Exception {

		createTestEnvironmentFolder();

		createTestVCSRepos();

		uploadVCSConfigFiles();

		createCredentialsFile();

		createReposFile();

		VCSRepositories.setConfigSource(new IConfigSource() {
			@Override
			public String getReposLocations() {
				return "file://localhost/" + getReposFile().getPath().replace("\\", "/");
			}

			@Override
			public String getCredentialsLocations() {
				return "file://localhost/" + getCredsFile().getPath().replace("\\", "/");
			}
		});

	}

	private void createReposFile() throws IOException {
		reposFile = new File(TEST_ENVIRONMENT_DIR, TEST_REPOS_FILE_NAME);
		reposFile.createNewFile();
		FileUtils.writeLines(reposFile,Arrays.asList(
				"eu.untill:(.*):",
				" url: " + new File(TEST_REMOTE_REPO_DIR).toURI().toURL().toString() + "$1-" + RANDOM_VCS_NAME_SUFFIX + ".git"));
	}

	private void createCredentialsFile() throws IOException {
		credsFile = new File(TEST_ENVIRONMENT_DIR, TEST_CREDENTIALS_FILE_NAME);
		credsFile.createNewFile();
	}

	private void uploadVCSConfigFiles() {
		unTillVCS.setFileContent(null, SCMWorkflow.VER_FILE_NAME, unTillVer.toString(), LogTag.SCM_VER + " ver file added");
		unTillVCS.setFileContent(null, SCMWorkflow.MDEPS_FILE_NAME,
				PRODUCT_UBL + ":" + ublVer.getSnapshot() + "\r\n" +
				PRODUCT_UNTILLDB + ":" + unTillDbVer.getSnapshot() + "\r\n", LogTag.SCM_IGNORE + " mdeps file added");

		ublVCS.setFileContent(null, SCMWorkflow.VER_FILE_NAME, ublVer.toString(), LogTag.SCM_VER + " ver file added");
		ublVCS.setFileContent(null, SCMWorkflow.MDEPS_FILE_NAME,
				PRODUCT_UNTILLDB + ":" + unTillDbVer.getSnapshot() + "\r\n", LogTag.SCM_IGNORE + " mdeps file added");

		unTillDbVCS.setFileContent(null, SCMWorkflow.VER_FILE_NAME, unTillDbVer.toString(), LogTag.SCM_VER + " ver file added");
	}

	private void createTestVCSRepos() throws Exception {
		IVCSWorkspace localVCSWorkspace = new VCSWorkspace();
		File unTillRemoteRepoDir = new File(TEST_REMOTE_REPO_DIR, "unTill-" + RANDOM_VCS_NAME_SUFFIX + ".git");
		File ublRemoteRepoDir = new File(TEST_REMOTE_REPO_DIR, "UBL-" + RANDOM_VCS_NAME_SUFFIX + ".git");
		File unTillRemoteDbRepoDir = new File(TEST_REMOTE_REPO_DIR, "unTillDb-" + RANDOM_VCS_NAME_SUFFIX + ".git");
		GitVCSUtils.createRepository(unTillRemoteRepoDir);
		GitVCSUtils.createRepository(ublRemoteRepoDir);
		GitVCSUtils.createRepository(unTillRemoteDbRepoDir);
		IVCSRepositoryWorkspace unTillVCSRepoWS = localVCSWorkspace
				.getVCSRepositoryWorkspace(StringUtils.removeEndIgnoreCase(unTillRemoteRepoDir.toURI().toURL().toString(), "/"));
		IVCSRepositoryWorkspace ublVCSRepoWS = localVCSWorkspace
				.getVCSRepositoryWorkspace(StringUtils.removeEndIgnoreCase(ublRemoteRepoDir.toURI().toURL().toString(), "/"));
		IVCSRepositoryWorkspace unTillDbVCSRepoWS = localVCSWorkspace
				.getVCSRepositoryWorkspace(StringUtils.removeEndIgnoreCase(unTillRemoteDbRepoDir.toURI().toURL().toString(), "/"));
		unTillVCS = new GitVCS(unTillVCSRepoWS);
		ublVCS = new GitVCS(ublVCSRepoWS);
		unTillDbVCS = new GitVCS(unTillDbVCSRepoWS);
	}

	private void createTestEnvironmentFolder() throws IOException {
		envDir = new File(TEST_ENVIRONMENT_DIR);
		if (envDir.exists()) {
			FileUtils.deleteDirectory(envDir);
		}
		envDir.mkdirs();
	}

	public IVCS getUnTillVCS() {
		return unTillVCS;
	}

	public IVCS getUblVCS() {
		return ublVCS;
	}

	public IVCS getUnTillDbVCS() {
		return unTillDbVCS;
	}

	public File getCredsFile() {
		return credsFile;
	}

	public File getReposFile() {
		return reposFile;
	}
	
	public VCSCommit generateLogTag(IVCS vcs, String branchName, String logTag) {
		return generateDummyContent(vcs, branchName, logTag);
	}

	public VCSCommit generateFeatureCommit(IVCS vcs, String branchName, String commitMessage) {
		return generateContent(vcs, branchName, TEST_FEATURE_FILE_NAME, "feature content", commitMessage);
	}
	
	public VCSCommit generateContent(IVCS vcs, String branchName, String fileName, String content, String logMessage) {
		return vcs.setFileContent(branchName, fileName, content, logMessage);
	}
	
	public VCSCommit generateDummyContent(IVCS vcs, String branchName, String logMessage) {
		return vcs.setFileContent(branchName, TEST_DUMMY_FILE_NAME, "dummy content " + UUID.randomUUID().toString(), logMessage);
	}

	public Version getUnTillVer() {
		return unTillVer;
	}

	public Version getUblVer() {
		return ublVer;
	}

	public Version getUnTillDbVer() {
		return unTillDbVer;
	}

	@Override
	public void close() throws Exception {
		if (envDir != null && envDir.exists()) {
			FileUtils.deleteDirectory(envDir);
		}
		VCSRepositories.setConfigSource(new EnvVarsConfigSource());
	}
}
