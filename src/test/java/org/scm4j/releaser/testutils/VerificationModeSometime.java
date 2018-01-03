package org.scm4j.releaser.testutils;

import org.mockito.internal.invocation.InvocationMatcher;
import org.mockito.internal.invocation.InvocationsFinder;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.internal.verification.api.VerificationData;
import org.mockito.invocation.Invocation;
import org.mockito.verification.VerificationMode;

import java.util.List;

import static org.mockito.exceptions.Reporter.wantedButNotInvoked;
import static org.mockito.internal.invocation.InvocationMarker.markVerified;

public class VerificationModeSometime implements VerificationMode {

	private final InvocationsFinder finder = new InvocationsFinder();

	public static VerificationModeSometime sometime() {
		return new VerificationModeSometime();
	}

	@Override
	public void verify(VerificationData data) {
		InvocationMatcher wantedMatcher = data.getWanted();
		List<Invocation> invocations = data.getAllInvocations();
		List<Invocation> chunk = finder.findInvocations(invocations,wantedMatcher);
		if (chunk.size() == 0) {
			throw wantedButNotInvoked(wantedMatcher);
		}
		markVerified(chunk.get(0), wantedMatcher);
	}

	@Override
	public VerificationMode description(String description) {
		return VerificationModeFactory.description(this, description);
	}
}
