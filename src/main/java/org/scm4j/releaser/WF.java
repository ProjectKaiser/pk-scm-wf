package org.scm4j.releaser;

import org.scm4j.commons.Version;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.ActionNone;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.DevelopBranchStatus;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.branch.ReleaseBranchStatus;
import org.scm4j.releaser.conf.*;
import org.scm4j.releaser.exceptions.EComponentConfig;
import org.scm4j.releaser.scmactions.ReleaseReason;
import org.scm4j.releaser.scmactions.SCMActionBuild;
import org.scm4j.releaser.scmactions.SCMActionFork;
import org.scm4j.releaser.scmactions.SCMActionTagRelease;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSTag;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class WF {

	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String VER_FILE_NAME = "version";
	public static final String DELAYED_TAGS_FILE_NAME = "delayed-tags.yml";
	public static final File BASE_WORKING_DIR = new File(System.getProperty("user.home"), ".scm4j");

	private final List<Option> options;

	public WF(List<Option> options) {
		this.options = options;
	}

	public WF() {
		this(new ArrayList<Option>());
	}

	public static List<Option> parseOptions(String[] args) {
		List<Option> options = new ArrayList<>();
		for (String arg : args) {
			if (Option.getArgsMap().containsKey(arg)) {
				options.add(Option.getArgsMap().get(arg));
			}
		}
		return options;
	}
	
	private boolean isNeedToFork(Component comp) {
		CurrentReleaseBranch crb = new CurrentReleaseBranch(comp);
		if (!crb.exists()) {
			return true;
		}
		
		Version ver = crb.getVersion();
		if (ver.getPatch().equals("0")) {
			return false;
		}
		
		DevelopBranch db = new DevelopBranch(comp);
		if (db.getStatus() == DevelopBranchStatus.MODIFIED) {
			return true;
		}
		
		List<Component> mDeps = db.getMDeps();
		for (Component mDep : mDeps) {
			if (isNeedToFork(mDep)) {
				return true;
			}
		}
		
		mDeps = crb.getMDeps();
		
	}
	
	public IAction getActionTree(Component comp) {
		
	}


}
