package org.scm4j.wf;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.scm4j.vcs.GitVCS;
import org.scm4j.vcs.GitVCSUtils;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.workingcopy.IVCSRepositoryWorkspace;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.api.workingcopy.VCSWorkspace;

public class TestEnvironment {
	public static final String TEST_REPOS_FILE_NAME = "repos";
	public static final String TEST_CREDENTIALS_FILE_NAME = "credentials";
	public static final String TEST_ENVIRONMENT_DIR = System.getProperty("java.io.tmpdir") + "scm4j-wf-test";
	public static final String TEST_ENVIRONMENT_URL = "file://localhost/" + TEST_ENVIRONMENT_DIR.replace("\\", "/");
	public static final String TEST_VCS_REPO_FILE_URL = "file://localhost/" + TEST_ENVIRONMENT_URL + "/vcs-repo";
	public static final String TEST_LOCAL_REPO_DIR = new File(TEST_ENVIRONMENT_DIR, "local-repos").getPath();
	public static final String TEST_REMOTE_REPO_DIR = new File(TEST_ENVIRONMENT_DIR, "remote-repos").getPath();

	private IVCS unTillVCS;
	private IVCS ublVCS;
	private IVCS unTillDbVCS;
	private File credsFile;
	private File reposFile;

	public void generateTestEnvironment() throws IOException {
		File envDir = new File(TEST_ENVIRONMENT_DIR);
		if (envDir.exists()) {
			FileUtils.deleteDirectory(envDir);
		}
		envDir.mkdirs();
		IVCSWorkspace localVCSWorkspace = new VCSWorkspace(TEST_ENVIRONMENT_DIR);

		File unTillRemoteRepoDir = new File(TEST_REMOTE_REPO_DIR, "unTill.git");
		File ublRemoteRepoDir = new File(TEST_REMOTE_REPO_DIR, "UBL.git");
		File unTillRemoteDbRepoDir = new File(TEST_REMOTE_REPO_DIR, "unTillDb.git");
		GitVCSUtils.createRepository(unTillRemoteRepoDir);
		GitVCSUtils.createRepository(ublRemoteRepoDir);
		GitVCSUtils.createRepository(unTillRemoteDbRepoDir);
		IVCSRepositoryWorkspace unTillVCSRepoWS = localVCSWorkspace
				.getVCSRepositoryWorkspace("file:///" + unTillRemoteRepoDir.getPath().replace("\\", "/"));
		IVCSRepositoryWorkspace ublVCSRepoWS = localVCSWorkspace
				.getVCSRepositoryWorkspace("file:///" + ublRemoteRepoDir.getPath().replace("\\", "/"));
		IVCSRepositoryWorkspace unTillDbVCSRepoWS = localVCSWorkspace
				.getVCSRepositoryWorkspace("file:///" + unTillRemoteDbRepoDir.getPath().replace("\\", "/"));
		unTillVCS = new GitVCS(unTillVCSRepoWS);
		ublVCS = new GitVCS(ublVCSRepoWS);
		unTillDbVCS = new GitVCS(unTillDbVCSRepoWS);

		unTillVCS.setFileContent(null, "ver", "1.123.3-SNAPSHOT", "ver file added");
		unTillVCS.setFileContent(null, "mdeps",
				"eu.untill:UBL:18.5-SNAPSHOT\r\n" + 
				"eu.untill:unTillDb:2.59.1-SNAPSHOT\r\n", "mdeps file added");

		ublVCS.setFileContent(null, "ver", "1.18.5-SNAPSHOT", "ver file added");
		ublVCS.setFileContent(null, "mdeps", "eu.untill:unTillDb:2.59.1-SNAPSHOT\r\n", "mdeps file added");

		unTillDbVCS.setFileContent(null, "ver", "2.59.1-SNAPSHOT", "ver file added");
		credsFile = new File(TEST_ENVIRONMENT_DIR, TEST_CREDENTIALS_FILE_NAME);
		credsFile.createNewFile();
		reposFile = new File(TEST_ENVIRONMENT_DIR, TEST_REPOS_FILE_NAME);
		reposFile.createNewFile();

		FileUtils.writeLines(reposFile,Arrays.asList(
				"[{\"name\": \"eu.untill:unTill\",\"url\": \"" + unTillVCS.getRepoUrl() + "\"},",
				"{\"name\": \"eu.untill:UBL\",\"url\": \"" + ublVCS.getRepoUrl() + "\"},",
				"{\"name\": \"eu.untill:unTillDb\",\"url\": \"" + unTillDbVCS.getRepoUrl() + "\"}]"));

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

}
