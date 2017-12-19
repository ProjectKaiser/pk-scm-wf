package org.scm4j.releaser;

import org.junit.Test;
import org.scm4j.releaser.branch.ReleaseBranchCurrent;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.conf.Component;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.WalkDirection;

import java.util.ArrayList;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class ExtendedStatusBuilderTest extends WorkflowTestBase {

	@Test
	public void testNoValueableCommitsAfterLastTagInterruption() throws Exception {
		forkAndBuild(compUnTillDb);

		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(compUnTillDb);
		Component mockedComp = spy(compUnTillDb.clone(crb.getVersion()));
		IVCS mockedVCS = spy(env.getUnTillDbVCS());
		doReturn(mockedVCS).when(mockedComp).getVCS();
		doReturn(new ArrayList<VCSCommit>()).when(mockedVCS)
				.getCommitsRange(anyString(), (String) isNull(), any(WalkDirection.class), anyInt());

		ExtendedStatusBuilder statusBuilder = new ExtendedStatusBuilder();
		statusBuilder.getAndCachePatchStatus(mockedComp, new CachedStatuses());
	}
}