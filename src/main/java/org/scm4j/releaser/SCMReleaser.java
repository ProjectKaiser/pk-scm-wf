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
import java.util.function.Supplier;

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
		
		calculatedResult.setBuildStatus(comp, () -> getBuildStatus(comp, calculatedResult, progress));
		
		progress.close();
		return new SCMActionRelease(calculatedResult.getReleaseBranch(comp), comp, childActions, actionKind, calculatedResult.getBuildStatus(comp), calculatedResult);
	}

	protected BuildStatus getBuildStatus(Component comp, CalculatedResult calculatedResult, IProgress progress) {
		Build mb = new Build(calculatedResult.getReleaseBranch(comp), comp, calculatedResult);
		return reportDuration(mb::getStatus, "status calculation", comp, progress);
	}
	
	private void calculateResultNoStatus(Component comp, CalculatedResult calculatedResult, IProgress progress) {
		
		if (Options.isPatch()) {
			ReleaseBranch rb = calculatedResult.setReleaseBranch(comp, () -> new ReleaseBranch(comp, comp.getCoords().getVersion()));
			calculatedResult.setMDeps(comp, rb::getMDeps);
			calculatedResult.setNeedsToFork(comp, () -> false);
			return;
		}
		
		ReleaseBranch rb = calculatedResult.setReleaseBranch(comp, () -> reportDuration(() -> new ReleaseBranch(comp), "release branch version calculation", comp, progress));
		if (calculatedResult.getMDeps(comp) == null) {
			boolean needToUseReleaseBranch = (comp.getVersion().isExact() || (!comp.getVersion().isExact() && !calculatedResult.setNeedsToFork(comp, () -> {
				Build mb = new Build(rb, comp, calculatedResult);
				return reportDuration(mb::isNeedToFork, "need to fork calculation", comp, progress);
			}))) && rb.exists();
			// untill has untilldb, ubl has untilldb. untill is BUILD_MDEPS, UBL has release branch but need to FORK. 
			// result: db for untill FORK, db for UBL is DONE prev version (mdep fro existing UBL RB is used) 
			// TODO: add test: untill build_mdeps, untill needs to be forked. UBL has release rbanch but has to be forked also. untilldbs must have the same status
			calculatedResult.setMDeps(comp, () -> reportDuration(() -> needToUseReleaseBranch ? rb.getMDeps() : new DevelopBranch(comp).getMDeps(),
					String.format("read mdeps from %s branch", needToUseReleaseBranch ? "release" : "develop"), comp, progress));
		}
	}
	
	public static <T> T reportDuration(Supplier<T> sup, String message, Component comp, IProgress progress) {
		long start = System.currentTimeMillis();
		T res = sup.get();
		progress.reportStatus(message + ": " + (comp == null ? "" : comp.getCoordsNoComment() + " ") + "in " + (System.currentTimeMillis() - start) + "ms");
		return res;
	}
	
	public static <T> void reportDuration(Runnable run, String message, Component comp, IProgress progress) {
		reportDuration(() -> {
			run.run();
			return null;
		}, message, comp, progress);
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
