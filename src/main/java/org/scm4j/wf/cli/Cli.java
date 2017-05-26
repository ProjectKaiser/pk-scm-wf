package org.scm4j.wf.cli;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
import org.scm4j.wf.Credentials;
import org.scm4j.wf.GsonUtils;
import org.scm4j.wf.ISCMWorkflow;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.VCSRepository;
import org.scm4j.wf.VCSType;

import com.google.gson.reflect.TypeToken;

public class Cli {
	
	public static final String WORKSPACE_DIR = System.getProperty("java.io.tmpdir") + "scm4j-wf-workspaces";
	private static Map<String, Credentials> credentials = new HashMap<>();
	private static Map<String, VCSRepository> vcsRepos = new HashMap<>();
	private static Credentials defaultCred;
	
    public static void main(String[] args) throws Exception {
    	
    	loadCredentials();
    	
    	IVCSWorkspace ws = new VCSWorkspace(WORKSPACE_DIR);
    	
    	loadRepos(ws);
    	
    	ISCMWorkflow wf = new SCMWorkflow(vcsRepos);
    	IAction action = wf.calculateProductionReleaseAction(null, args[0]);
    	
    	PrintAction pa = new PrintAction();
    	pa.print(System.out, action);
    	
    	try (IProgress progress = new ProgressConsole(">>>" + action.getName())) {
    		try {
	    		action.execute(progress);
	    	} finally {
	    		progress.reportStatus("<<<" + action.getName());
	    	}
    	}
    	
    }

	private static void loadCredentials() throws Exception {
		String storeUrl = System.getenv("SCM4J_CREDENTIALS_URL");
    	URL url = new URL(storeUrl);
    	String credsJson;
    	try (InputStream inputStream = url.openStream()) {
    		credsJson = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
    	} catch (Exception e) {
    		return;
    	}
    	
    	Type type = new TypeToken<List<Credentials>>() {}.getType();
    	List<Credentials> creds = GsonUtils.fromJson(credsJson, type);
	
    	for (Credentials cred : creds) {
    		credentials.put(cred.getName(), cred);
    		if (cred.getIsDefault()) {
    			defaultCred = cred;
    		}
    	}
	}

	private static void loadRepos(IVCSWorkspace ws) throws Exception {
		String storeUrl = System.getenv("SCM4J_VCS_REPOS_URL");
    	URL url = new URL(storeUrl);
    	String vcsReposJson;
    	try (InputStream inputStream = url.openStream()) {
    		vcsReposJson = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
    	}
    	
    	Type type = new TypeToken<List<VCSRepository>>() {}.getType();
    	List<VCSRepository> repos = GsonUtils.fromJson(vcsReposJson, type);
    	
    	for (VCSRepository repo : repos) {
    		if (repo.getType() == null) {
    			repo.setType(getVCSType(repo.getUrl()));
    		}
    		if (repo.getCredentials() == null) {
    			repo.setCredentials(defaultCred);
    		} else {
    			repo.setCredentials(credentials.get(repo.getCredentials().getName()));
    		}
    		repo.setWorkspace(ws.getVCSRepositoryWorkspace(repo.getUrl()));
    		vcsRepos.put(repo.getName(), repo);
    	}
	}

	private static VCSType getVCSType(String url) {
		if (url.contains(".git")) {
			return VCSType.GIT;
		}
		return VCSType.SVN;
	}
	

}
