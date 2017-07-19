package org.scm4j.wf.scmactions;

import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.wf.actions.ActionAbstract;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.actions.results.ActionResultTag;
import org.scm4j.wf.conf.Component;

public class SCMActionUseExistingTag extends ActionAbstract {
	
	private VCSTag tag;

	public SCMActionUseExistingTag(Component dep, List<IAction> childActions, VCSTag tag) {
		super(dep, childActions);
		this.tag = tag;
	}
	
	public VCSTag getTag() {
		return tag;
	}
	
	@Override
	public String toString() {
		return "using existing tag for " + getName() + ": " + tag.toString();
	}

	@Override
	public Object execute(IProgress progress) {
		progress.reportStatus(toString());
		ActionResultTag res = new ActionResultTag(getName(), tag);
		return res;
	}

	
}
