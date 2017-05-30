package org.scm4j.progress;

public interface IProgress extends AutoCloseable {
	IProgress createNestedProgress(String name);

	void reportStatus(String status);
}
