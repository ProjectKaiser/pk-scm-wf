package org.scm4j.wf.cli;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.scm4j.actions.IAction;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.api.workingcopy.VCSWorkspace;
import org.scm4j.wf.Credentials;
import org.scm4j.wf.GsonUtils;
import org.scm4j.wf.ISCMWorkflow;
import org.scm4j.wf.IVCSFactory;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.VCSRepository;
import org.scm4j.wf.VCSType;

import com.google.gson.reflect.TypeToken;

public class Cli {
	
	public static final String WORKSPACE_DIR = System.getProperty("java.io.tmpdir") + "scm4j-wf-workspaces";
	private static Map<String, Credentials> credentials;
	private static Map<String, VCSRepository> vcsRepos;
	private static Credentials defaultCred;
	
    public static void main(String[] args) throws Exception {
    	
    	loadCredentials();
    	
    	loadRepos();

    	VCSRepository repo = vcsRepos.get(args[1]);
    	IVCSWorkspace ws = new VCSWorkspace(WORKSPACE_DIR);
    	
    	IVCS vcs = IVCSFactory.getIVCS(ws, repo);
    	
    	ISCMWorkflow wf = new SCMWorkflow(credentials, vcsRepos);
    	IAction action = wf.calculateProductionReleaseAction(vcs, null);

    	System.out.println(action.toString());
    }

	private static void loadCredentials() throws Exception {
		String storeUrl = System.getenv("SCM4J_CREDENTIALS_URL");
    	URL url = new URL(storeUrl);
    	String credsJson;
    	try (InputStream inputStream = url.openStream()) {
    		credsJson = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
    	}
    	
    	Type token = new TypeToken<List<Credentials>>() {}.getType();
    	List<Credentials> creds = GsonUtils.fromJson(credsJson, token);
	
    	for (Credentials cred : creds) {
    		credentials.put(cred.getName(), cred);
    		if (cred.getIsDefault()) {
    			defaultCred = cred;
    		}
    	}
	}

	private static void loadRepos() throws Exception {
		String storeUrl = System.getenv("SCM4J_VCS_REPOS_URL");
    	URL url = new URL(storeUrl);
    	String vcsReposJson;
    	try (InputStream inputStream = url.openStream()) {
    		vcsReposJson = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
    	}
    	
    	Type token = new TypeToken<List<VCSRepository>>() {}.getType();
    	List<VCSRepository> repos = GsonUtils.fromJson(vcsReposJson, token);
    	
    	for (VCSRepository repo : repos) {
    		if (repo.getType() == null) {
    			repo.setType(getVCSType(repo.getUrl()));
    		}
    		if (repo.getCredentials() == null) {
    			repo.setCredentials(defaultCred);
    		} else {
    			repo.setCredentials(credentials.get(repo.getCredentials().getName()));
    		}
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
