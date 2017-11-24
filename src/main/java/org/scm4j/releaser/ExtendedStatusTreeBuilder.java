package org.scm4j.releaser;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.scm4j.commons.coords.Coords;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.Options;

public class ExtendedStatusTreeBuilder {
	
	private final CalculatedResult calculatedResult;
	
	public ExtendedStatusTreeBuilder(CalculatedResult calculatedResult) {
		this.calculatedResult = calculatedResult;
	}
	
	public Map<Coords, ExtendedStatusTreeNode> getStatusTree(Component comp) {
		IProgress progress = new ProgressConsole();
		Map<Coords, ExtendedStatusTreeNode> res = new ConcurrentHashMap<>();
		
		List<Component> mdeps;
		Utils.async(mdeps, (mdep) -> {
			res.
		})
		for (Component mdep : mdeps) {
			
		}
		
		
	}
	
	public Map<Coords, ExtendedStatusTreeNode> getStatusTree(Component comp) {
		
		/*
		 * получим
		 */
		
		if (Options.isPatch()) {
			ReleaseBranch rb = calculatedResult.setReleaseBranch(comp, () -> new ReleaseBranch(comp, comp.getCoords().getVersion()));
			calculatedResult.setMDeps(comp, rb::getMDeps);
			calculatedResult.setNeedsToFork(comp, () -> false);
			return;
		}
		
		
	}

}
