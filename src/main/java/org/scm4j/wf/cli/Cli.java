package org.scm4j.wf.cli;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.scm4j.actions.IAction;
import org.scm4j.actions.PrintAction;
import org.scm4j.progress.IProgress;
import org.scm4j.progress.ProgressConsole;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.api.workingcopy.VCSWorkspace;
import org.scm4j.wf.ISCMWorkflow;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.model.Credentials;
import org.scm4j.wf.model.VCSRepository;

public class Cli {

	public static final String WORKSPACE_DIR = new File(System.getProperty("user.home"), ".scm4j").getPath();
	private static Map<String, Credentials> credentials = new HashMap<>();
	private static Map<String, VCSRepository> vcsRepos = new HashMap<>();

	public static void main(String[] args) throws Exception {

		loadCredentials();

		IVCSWorkspace ws = new VCSWorkspace(WORKSPACE_DIR);

		loadRepos(ws);
		
		String depName = args[0];

		ISCMWorkflow wf = new SCMWorkflow(depName, vcsRepos);
		IAction action = wf.getProductionReleaseAction();

		PrintAction pa = new PrintAction();
		pa.print(System.out, action);

		try (IProgress progress = new ProgressConsole(action.getName(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
	}

	private static void loadCredentials() throws Exception {
		String storeUrlsStr = System.getenv("SCM4J_CREDENTIALS");
		String[] storeUrls = storeUrlsStr.split(";");
		for (String storeUrl : storeUrls) {
			URL url = new URL(storeUrl);
			String credsJson;
			try (InputStream inputStream = url.openStream()) {
				credsJson = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
			} catch (Exception e) {
				return;
			}

			for (Credentials cred : Credentials.fromJson(credsJson)) {
				credentials.put(cred.getName(), cred);
			}
		}
	}

	private static void loadRepos(IVCSWorkspace ws) throws Exception {
		String storeUrlsStr = System.getenv("SCM4J_VCS_REPOS");
		String[] storeUrls = storeUrlsStr.split(";");
		for (String storeUrl : storeUrls) {
			URL url = new URL(storeUrl);
			String vcsReposJson;
			try (InputStream inputStream = url.openStream()) {
				vcsReposJson = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
			}
			List<VCSRepository> repos = VCSRepository.fromJson(vcsReposJson, new ArrayList<>(credentials.values()), ws);
			for (VCSRepository repo : repos) {
				vcsRepos.put(repo.getName(), repo);
			}
		}
	}
}
