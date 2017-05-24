package org.scm4j.progress;

public interface IProgress {
	IProgress createNestedProgress(String name);
	IProgress getParent();
	String getName();
	
	void reportStatus(String status);
	void close();
}
