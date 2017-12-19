package org.scm4j.releaser;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.scm4j.commons.Version;
import org.scm4j.releaser.builders.BuilderFactory;
import org.scm4j.releaser.builders.TestBuilder;
import org.scm4j.releaser.conf.EnvVarsConfigSource;
import org.scm4j.releaser.conf.IConfigSource;
import org.scm4j.releaser.conf.VCSRepositories;
import org.scm4j.releaser.conf.VCSType;
import org.scm4j.vcs.GitVCS;
import org.scm4j.vcs.GitVCSUtils;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.workingcopy.IVCSRepositoryWorkspace;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.api.workingcopy.VCSWorkspace;
import org.scm4j.vcs.svn.SVNVCS;
import org.scm4j.vcs.svn.SVNVCSUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

public class TestEnvironment implements AutoCloseable {
	public static final String TEST_REPOS_FILE_NAME = "repos";
	public static final String TEST_CREDENTIALS_FILE_NAME = "credentials";
	public static final String TEST_ENVIRONMENT_DIR = new File(System.getProperty("java.io.tmpdir"),
			"scm4j-releaser-test").getPath();
	public static final String TEST_VCS_WORKSPACES_DIR = new File(TEST_ENVIRONMENT_DIR, "vcs-workspaces").getPath();
	public static final String TEST_REMOTE_REPO_DIR = new File(TEST_ENVIRONMENT_DIR, "remote-repos").getPath();
	public static final String TEST_FEATURE_FILE_NAME = "feature.txt";
	public static final String TEST_DUMMY_FILE_NAME = "dummy.txt";

	public static final String PRODUCT_UNTILL = "eu.untill:unTill";
	public static final String PRODUCT_UBL = "eu.untill:UBL";
	public static final String PRODUCT_UNTILLDB = "eu.untill:unTillDb";

	public final String RANDOM_VCS_NAME_SUFFIX;

	private static final VCSType TESTING_VCS = VCSType.GIT;
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
		generateTestEnvironmentNoVCS();

		createTestVCSRepos();

		uploadVCSConfigFiles();
	}

	public void generateTestEnvironmentNoVCS() throws Exception {
		createTestEnvironmentFolder();

		createCredentialsFile();

		createReposFile();

		VCSRepositories.setConfigSource(new IConfigSource() {
			@Override
			public String getReposLocations() {
				return getReposFile().toString().replace("\\", "/");
			}

			@Override
			public String getCredentialsLocations() {
				return getCredsFile().toString().replace("\\", "/");
			}
		});
	}

	private void createReposFile() throws IOException {
		reposFile = new File(TEST_ENVIRONMENT_DIR, TEST_REPOS_FILE_NAME);
		reposFile.createNewFile();
		String url = new File(TEST_REMOTE_REPO_DIR, "$1-" + RANDOM_VCS_NAME_SUFFIX).toURI().toURL().toString();
		if (TESTING_VCS == VCSType.SVN) {
			url = url.replace("file:/", "file://");
		}
		FileUtils.writeLines(reposFile, Arrays.asList(
				"!!omap", 
				"- eu.untill:(.*):", 
				"   url: " + url,
				"   releaseCommand: " + BuilderFactory.SCM4J_BUILDER_CLASS_STRING + TestBuilder.class.getName(),
				"   type: " + TESTING_VCS.toString().toLowerCase(),
				"   releaseBranchPrefix: release/B"));
	}

	private void createCredentialsFile() throws IOException {
		credsFile = new File(TEST_ENVIRONMENT_DIR, TEST_CREDENTIALS_FILE_NAME);
		credsFile.createNewFile();
	}

	private void uploadVCSConfigFiles() {
		unTillVCS.setFileContent(null, Utils.VER_FILE_NAME, unTillVer.toString(),
				LogTag.SCM_IGNORE + " ver file added");
		unTillVCS.setFileContent(null,
				Utils.MDEPS_FILE_NAME, PRODUCT_UBL + ":" + ublVer.getSnapshot() + " # comment 1\r\n"
						+ PRODUCT_UNTILLDB + ":" + unTillDbVer.getSnapshot() + "# comment 2\r\n",
				LogTag.SCM_IGNORE + " mdeps file added");

		ublVCS.setFileContent(null, Utils.VER_FILE_NAME, ublVer.toString(),
				LogTag.SCM_IGNORE + " ver file added");
		ublVCS.setFileContent(null, Utils.MDEPS_FILE_NAME,
				PRODUCT_UNTILLDB + ":" + unTillDbVer.getSnapshot() + "#comment 3\r\n",
				LogTag.SCM_IGNORE + " mdeps file added");

		unTillDbVCS.setFileContent(null, Utils.VER_FILE_NAME, unTillDbVer.toString(),
				LogTag.SCM_IGNORE + " ver file added");
	}

	private void createTestVCSRepos() throws Exception {
		IVCSWorkspace localVCSWorkspace = new VCSWorkspace(TEST_VCS_WORKSPACES_DIR);
		File unTillRemoteRepoDir = new File(TEST_REMOTE_REPO_DIR, "unTill-" + RANDOM_VCS_NAME_SUFFIX);
		File ublRemoteRepoDir = new File(TEST_REMOTE_REPO_DIR, "UBL-" + RANDOM_VCS_NAME_SUFFIX);
		File unTillRemoteDbRepoDir = new File(TEST_REMOTE_REPO_DIR, "unTillDb-" + RANDOM_VCS_NAME_SUFFIX);
		IVCSRepositoryWorkspace unTillVCSRepoWS;
		IVCSRepositoryWorkspace ublVCSRepoWS;
		IVCSRepositoryWorkspace unTillDbVCSRepoWS;
		switch (TESTING_VCS) {
		case GIT:
			GitVCSUtils.createRepository(unTillRemoteRepoDir);
			GitVCSUtils.createRepository(ublRemoteRepoDir);
			GitVCSUtils.createRepository(unTillRemoteDbRepoDir);
			unTillVCSRepoWS = localVCSWorkspace.getVCSRepositoryWorkspace(
					StringUtils.removeEndIgnoreCase(unTillRemoteRepoDir.toURI().toURL().toString(), "/"));
			ublVCSRepoWS = localVCSWorkspace.getVCSRepositoryWorkspace(
					StringUtils.removeEndIgnoreCase(ublRemoteRepoDir.toURI().toURL().toString(), "/"));
			unTillDbVCSRepoWS = localVCSWorkspace.getVCSRepositoryWorkspace(
					StringUtils.removeEndIgnoreCase(unTillRemoteDbRepoDir.toURI().toURL().toString(), "/"));
			unTillVCS = new GitVCS(unTillVCSRepoWS);
			ublVCS = new GitVCS(ublVCSRepoWS);
			unTillDbVCS = new GitVCS(unTillDbVCSRepoWS);
			break;
		case SVN:
			SVNVCSUtils.createRepository(unTillRemoteRepoDir);
			SVNVCSUtils.createRepository(ublRemoteRepoDir);
			SVNVCSUtils.createRepository(unTillRemoteDbRepoDir);
			unTillVCSRepoWS = localVCSWorkspace.getVCSRepositoryWorkspace(
					StringUtils.removeEndIgnoreCase(unTillRemoteRepoDir.toURI().toURL().toString(), "/")
							.replace("file:/", "file://"));
			ublVCSRepoWS = localVCSWorkspace.getVCSRepositoryWorkspace(
					StringUtils.removeEndIgnoreCase(ublRemoteRepoDir.toURI().toURL().toString(), "/").replace("file:/",
							"file://"));
			unTillDbVCSRepoWS = localVCSWorkspace.getVCSRepositoryWorkspace(
					StringUtils.removeEndIgnoreCase(unTillRemoteDbRepoDir.toURI().toURL().toString(), "/")
							.replace("file:/", "file://"));
			unTillVCS = new SVNVCS(unTillVCSRepoWS, null, null);
			SVNVCSUtils.createFolderStructure((SVNVCS) unTillVCS, "initial commit");
			ublVCS = new SVNVCS(ublVCSRepoWS, null, null);
			SVNVCSUtils.createFolderStructure((SVNVCS) ublVCS, "initial commit");
			unTillDbVCS = new SVNVCS(unTillDbVCSRepoWS, null, null);
			SVNVCSUtils.createFolderStructure((SVNVCS) unTillDbVCS, "initial commit");
			break;
		default:
			throw new IllegalStateException("unsupported testing vcs type: " + TESTING_VCS.toString());
		}
	}

	private void createTestEnvironmentFolder() throws Exception {
		envDir = new File(TEST_ENVIRONMENT_DIR);
		if (envDir.exists()) {
			Utils.waitForDeleteDir(envDir);
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

	public VCSCommit generateFeatureCommit(IVCS vcs, String branchName, String commitMessage) {
		return generateContent(vcs, branchName, TEST_FEATURE_FILE_NAME,
				"feature content " + UUID.randomUUID().toString(), commitMessage);
	}

	public VCSCommit generateContent(IVCS vcs, String branchName, String fileName, String content, String logMessage) {
		return vcs.setFileContent(branchName, fileName, content, logMessage);
	}

	public VCSCommit generateDummyContent(IVCS vcs, String branchName, String logMessage) {
		return vcs.setFileContent(branchName, TEST_DUMMY_FILE_NAME, "dummy content " + UUID.randomUUID().toString(),
				logMessage);
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
			Utils.waitForDeleteDir(envDir);
		}
		VCSRepositories.setConfigSource(new EnvVarsConfigSource());
		VCSRepositories.resetDefault();
	}
}
