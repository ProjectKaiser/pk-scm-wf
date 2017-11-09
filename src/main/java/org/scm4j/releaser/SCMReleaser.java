package org.scm4j.releaser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

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
		return getActionTree(new Component(coords), actionKind, new ConcurrentHashMap<String, CalculatedResult>());
	}
	
	public IAction getActionTree(Component comp, ActionKind actionKind) throws Exception {
		Options.setIsPatch(comp.getVersion().isExact());
		return getActionTree(comp, actionKind, new ConcurrentHashMap<String, CalculatedResult>());
	}

	public IAction getActionTree(Component comp, final ActionKind actionKind, final ConcurrentHashMap<String, CalculatedResult> calculatedStatuses) throws Exception {
		List<IAction> childActions = new ArrayList<>();
		CalculatedResult cr = getCalculatedResult(comp, calculatedStatuses);
		
		Map<Component, Object> res = new ConcurrentHashMap<>();
		ForkJoinPool myPool = new ForkJoinPool(8);
		myPool.submit(() ->
			cr.getMDeps().parallelStream().forEach((mdep) -> {
				try {
					res.put(mdep, getActionTree(mdep, actionKind, calculatedStatuses) );
				} catch (Exception e) {
					res.put(mdep,  e);
				}
			})
		).get();
		
		myPool.shutdown();
		
		
		for (Component mdep : cr.getMDeps()) {
			if (res.get(mdep) instanceof Exception) {
				throw (Exception) res.get(mdep);
			} else {
				childActions.add((IAction) res.get(mdep));
			}
		}
		
		return new SCMActionRelease(cr.getReleaseBranch(), childActions, actionKind, cr.getBuildStatus());
	}

	private CalculatedResult getCalculatedResult(Component comp, Map<String, CalculatedResult> calculatedStatuses) throws Exception {
		CalculatedResult cr = calculatedStatuses.get(comp.getVcsRepository().getUrl());
		if (cr != null) {
			return cr;
		}
		
		List<Component> mDeps;
		ReleaseBranch rb;
		BuildStatus bs;
		if (Options.isPatch()) {
			rb = new ReleaseBranch(comp, comp.getCoords().getVersion());
			mDeps = rb.getMDeps();
			bs = getBuildStatus(rb);
			cr = new CalculatedResult(rb, bs, mDeps);
			calculatedStatuses.put(comp.getVcsRepository().getUrl(), cr);
			return cr;
		}
		
		IProgress progress = new ProgressConsole();
		// If we are build, build_mdeps or actualize_patches then we need to use mdeps from release branches to show what versions we are going to build or actualize
		progress.startTrace(String.format("analyzing %s, url %s:\r\n", comp, comp.getVcsRepository().getUrl()));
		progress.startTrace("determining release branch version for " + comp.getCoordsNoComment() + "... ");
		rb = new ReleaseBranch(comp);
		progress.endTrace("done");
		bs = getBuildStatus(rb);	
		if (bs == BuildStatus.FORK) {
			// untill has untilldb, ubl has untilldb. untill is BUILD_MDEPS, UBL has release branch but need to FORK. 
			// result: regressinon for untill FORK, regiression for UBL is DONE prev version (mdep fro existing UBL RB is used) 
			// TODO: add test: untill build_mdeps, untill needs to be forked. UBL has release rbanch but has to be forked also. untilldbs must have the same status
			progress.startTrace("reading mdeps from develop branch... ");
			mDeps = new DevelopBranch(comp).getMDeps();
			progress.endTrace("done");
		} else {
			if (rb.exists()) {
				progress.startTrace("reading mdeps from release branch " + rb.getName() + "... ");
				mDeps = rb.getMDeps();
				progress.endTrace("done");
			} else {
				progress.startTrace("reading mdeps from develop branch... ");
				mDeps = new DevelopBranch(comp).getMDeps();
				progress.endTrace("done");
			}
		}
		cr = new CalculatedResult(rb, bs, mDeps);
		calculatedStatuses.put(comp.getVcsRepository().getUrl(), cr);
		progress.close();
		return cr;
	}

	protected BuildStatus getBuildStatus(ReleaseBranch rb) throws Exception {
		Build mb = new Build(rb);
		IProgress progress = new ProgressConsole();
		progress.startTrace("calculating status for " + rb.getComponent().getName() + "... ");
		BuildStatus mbs = mb.getStatus();
		progress.endTrace("done");
		progress.close();
		return mbs;
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
		return new SCMActionTag(new ReleaseBranch(comp), childActions);
	}
}
