package org.scm4j.releaser;

import java.util.Map;

import org.scm4j.commons.coords.Coords;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.Options;

public class ExtendedStatusTreeBuilder {
	
	public Map<Coords, ExtendedStatusTreeNode> getStatusTree(Component comp) {
		
		IProgress progress = new ProgressConsole();
		
		
		
		
		if (Options.isPatch()) {
			ReleaseBranch rb = calculatedResult.setReleaseBranch(comp, () -> new ReleaseBranch(comp, comp.getCoords().getVersion()));
			calculatedResult.setMDeps(comp, rb::getMDeps);
			calculatedResult.setNeedsToFork(comp, () -> false);
			return;
		}
		
		
	}

}
