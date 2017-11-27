package org.scm4j.releaser;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.Options;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.releaser.scmactions.SCMActionRelease;
import org.scm4j.releaser.scmactions.SCMActionTag;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SCMReleaser {

	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String VER_FILE_NAME = "version";
	public static final String DELAYED_TAGS_FILE_NAME = "delayed-tags.yml";
	public static final File BASE_WORKING_DIR = new File(System.getProperty("user.home"), ".scm4j");

	public IAction getActionTree(String coords) throws Exception {
		return getActionTree(new Component(coords));
	}
	
	public IAction getActionTree(Component comp) throws Exception {
		return getActionTree(comp, ActionKind.FULL);
	}
	
	public IAction getActionTree(String coords, ActionKind actionKind) throws Exception {
		Component comp = new Component(coords);
		Options.setIsPatch(comp.getVersion().isExact());
		return getActionTree(new Component(coords), actionKind, new CalculatedResult());
	}
	
	public IAction getActionTree(Component comp, ActionKind actionKind) throws Exception {
		Options.setIsPatch(comp.getVersion().isExact());
		return getActionTree(comp, actionKind, new CalculatedResult());
	}

	public IAction getActionTree(Component comp, ActionKind actionKind, CalculatedResult calculatedResult) throws Exception {
		
		List<IAction> childActions = new ArrayList<>();
		IProgress progress = new ProgressConsole();
		calculateResultNoStatus(comp, calculatedResult, progress);
		
		for (Component mdep : calculatedResult.getMDeps(comp)) {
			childActions.add(getActionTree(mdep, actionKind, calculatedResult));
		}
		
		calculatedResult.setBuildStatus(comp, () -> getBuildStatus(comp, calculatedResult), progress);
		
		progress.close();
		return new SCMActionRelease(calculatedResult.getReleaseBranch(comp), comp, childActions, actionKind, calculatedResult.getBuildStatus(comp), calculatedResult);
	}
	
	public ExtendedStatus getExtendedStatus(Component comp, ActionKind actionKind, CalculatedResult calculatedResult) throws Exception {
		List<IAction> childActions = new ArrayList<>();
		IProgress progress = new ProgressConsole();
		// сначала надо получить список mdeps
		
		calculateResultNoStatus(comp, calculatedResult, progress);
		
		for (Component mdep : calculatedResult.getMDeps(comp)) {
			childActions.add(getActionTree(mdep, actionKind, calculatedResult));
		}
		
		
		
		calculatedResult.setBuildStatus(comp, () -> getBuildStatus(comp, calculatedResult), progress);
		
		progress.close();
		return new ExtendedStatus();
	}
	
	

	protected BuildStatus getBuildStatus(Component comp, CalculatedResult calculatedResult) {
		Build mb = new Build(calculatedResult.getReleaseBranch(comp), comp, calculatedResult);
		return mb.getStatus();
	}
	
	/**
	 * 
	 * Where to take mdeps from?
		<li> (Version is not known) Fork needed => from Develop Branch
		<li> From RB (existance is checked above)
	 */
	private void calculateResultNoStatus(Component comp, CalculatedResult calculatedResult, IProgress progress) {
		
		if (Options.isPatch()) {
			ReleaseBranch rb = calculatedResult.setReleaseBranch(comp, () -> new ReleaseBranch(comp, comp.getCoords().getVersion()));
			calculatedResult.setMDeps(comp, rb::getMDeps);
			calculatedResult.setNeedsToFork(comp, () -> false);
			return;
		}
		
		ReleaseBranch rb = calculatedResult.setReleaseBranch(comp, () -> new ReleaseBranch(comp), progress);
		if (calculatedResult.getMDeps(comp) == null) {
			boolean needToUseDevelopBranch;
			if (comp.getVersion().isExact()) {
				needToUseDevelopBranch = false;
			} else {
				needToUseDevelopBranch = calculatedResult.setNeedsToFork(comp, () -> {
					Build mb = new Build(rb, comp, calculatedResult);
					return mb.isNeedToFork();
				}, progress);
			}
			
			calculatedResult.setMDeps(comp,
					() -> needToUseDevelopBranch ? new DevelopBranch(comp).getMDeps() : rb.getMDeps(), progress);
		}
	}
	
	public static TagDesc getTagDesc(String verStr) {
		String tagMessage = verStr + " release";
		return new TagDesc(verStr, tagMessage);
	}
	
	public IAction getTagActionTree(Component comp) {
		List<IAction> childActions = new ArrayList<>();
		DevelopBranch db = new DevelopBranch(comp);
		List<Component> mDeps = db.getMDeps();

		for (Component mDep : mDeps) {
			childActions.add(getTagActionTree(mDep));
		}
		return new SCMActionTag(new ReleaseBranch(comp), comp, childActions);
	}
}
