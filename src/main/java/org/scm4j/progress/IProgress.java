package org.scm4j.progress;

public interface IProgress extends AutoCloseable {
	IProgress createNestedProgress(String name);
	IProgress getParent();
	String getName();
	
	void reportStatus(String status);
}
