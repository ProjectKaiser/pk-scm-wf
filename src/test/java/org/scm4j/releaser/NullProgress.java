package org.scm4j.releaser;

import org.scm4j.commons.progress.IProgress;

public class NullProgress implements IProgress {

	@Override
	public void close() throws Exception {
	}

	@Override
	public IProgress createNestedProgress(String name) {
		return new NullProgress();
	}

	@Override
	public void reportStatus(String status) {
	}

	@Override
	public void trace(String message) {
	}

	@Override
	public void error(String message) {
	}
}
