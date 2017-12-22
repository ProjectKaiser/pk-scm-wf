package org.scm4j.releaser.cli;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.scm4j.commons.CommentedString;
import org.scm4j.commons.RegexConfig;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.conf.IConfig;
import org.scm4j.releaser.conf.URLContentLoader;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.api.workingcopy.VCSWorkspace;

public class DefaultConfig implements IConfig {
	
	public static final File PRIORITY_CC_FILE = new File(Utils.BASE_WORKING_DIR, "cc.yml");
	public static final File CC_URLS_FILE = new File(Utils.BASE_WORKING_DIR, "cc");
	public static final File CREDENTIALS_FILE = new File(Utils.BASE_WORKING_DIR, "credentials.yml");
	public static final String DEFAULT_VCS_WORKSPACE_DIR = new File(Utils.BASE_WORKING_DIR,
			"releaser-vcs-workspaces").getPath();
	
	private static final String URL_SEPARATOR = URLContentLoader.URL_SEPARATOR;
	
	private final IEnvVarsSource envVarsSource;
	
	public DefaultConfig() {
		this(new EnvVarsSource());
	}
	
	public DefaultConfig(IEnvVarsSource envVarsSource) {
		this.envVarsSource = envVarsSource;
	}

	@Override
	public RegexConfig getRepoConfig() {
		try {
			RegexConfig res = new RegexConfig();
			String separatedUrls = envVarsSource.getCCUrls();
			if (separatedUrls != null) {
				res.loadFromYamlUrls(separatedUrls);
				return res;
			}
			
			StringBuilder urlsSB = new StringBuilder();
			
			if (PRIORITY_CC_FILE.exists()) {
				urlsSB.append(PRIORITY_CC_FILE.toString() + URL_SEPARATOR);
			}
			
			if (CC_URLS_FILE.exists()) {
				List<String> lines = FileUtils.readLines(CC_URLS_FILE, StandardCharsets.UTF_8);
				for (String line : lines) {
					CommentedString cs = new CommentedString(line);
					if (cs.isValuable()) {
						urlsSB.append(cs.getStrNoComment() + URL_SEPARATOR);
					}
				}
			}
			
			if (urlsSB.length() > 0) {
				urlsSB.setLength(urlsSB.length() - URL_SEPARATOR.length());
			}
			
			res.loadFromYamlUrls(urlsSB.toString());
			return res;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public RegexConfig getCredentialsConfig() {
		RegexConfig res = new RegexConfig();
		String credentialsUrl = envVarsSource.getCredsUrl();
		try {
			if (credentialsUrl != null) {
				res.loadFromYamlUrls(credentialsUrl);
			} else if (CREDENTIALS_FILE.exists()) {
				res.loadFromYamlUrls(CREDENTIALS_FILE.toString());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return res;
	}

	@Override
	public IVCSWorkspace getWS() {
		return getDefaultWS();
	}

	@Override
	public Boolean isConfiguredByEnvironment() {
		return envVarsSource.getCCUrls() != null || envVarsSource.getCredsUrl() != null;
	}

	public static IVCSWorkspace getDefaultWS() {
		return new VCSWorkspace(DEFAULT_VCS_WORKSPACE_DIR);
	}
}
