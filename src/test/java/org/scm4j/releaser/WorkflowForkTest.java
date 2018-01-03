package org.scm4j.releaser;

import com.google.common.base.Strings;
import org.junit.Test;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranchCurrent;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class WorkflowForkTest extends WorkflowTestBase {
	
	@Test
	public void testLockMDepsIfNotLocked() {
		fork(compUBL);
		
		// simulate mdeps not locked
		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(repoUBL);
		MDepsFile mdf = new MDepsFile(env.getUblVCS().getFileContent(crb.getName(), Utils.MDEPS_FILE_NAME, null));
		mdf.replaceMDep(mdf.getMDeps().get(0).clone(""));
		env.getUblVCS().setFileContent(crb.getName(), Utils.MDEPS_FILE_NAME, mdf.toFileContent(), "mdeps not locked");
		
		// UBL should lock its mdeps
		IAction action = execAndGetActionFork(compUBL);
		assertThatAction(action, allOf(
				hasProperty("bsFrom", equalTo(BuildStatus.LOCK)),
				hasProperty("bsTo", equalTo(BuildStatus.LOCK))), compUBL);

		// check UBL mdeps locked
		checkUBLMDepsVersions(1);
	}

	@Test
	public void testMDepsFileFormatSaving() {
		String newMDepsFileContent = getMDepsFileTestContent(ReleaseBranchFactory.getMDepsDevelop(repoUnTill), true);
		env.getUnTillVCS().setFileContent(null, Utils.MDEPS_FILE_NAME, newMDepsFileContent, "mdeps file changed");

		fork(compUnTill);

		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(repoUnTill);
		String actualMDepsFileContent = env.getUnTillVCS().getFileContent(crb.getName(), Utils.MDEPS_FILE_NAME, null);
		String expectedMDepsFileContent = getMDepsFileTestContent(crb.getMDeps(), false);
		assertEquals(expectedMDepsFileContent, actualMDepsFileContent);
	}

	private String getMDepsFileTestContent(List<Component> mdeps, boolean addJunk) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println();
		pw.println("        # my cool comment ");
		pw.println();
		if (addJunk) {
			pw.print("  ");
		}
		int count = 1;
		for(Component mdep : mdeps) {
			if (addJunk) {
				pw.println(Strings.repeat("\t", count) + Strings.repeat(" ", count) + mdep.toString() + " # " + count);
			} else {
				pw.println(mdep.toString());
			}

			count++;
		}
		return sw.toString();
	}
}

