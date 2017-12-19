package org.scm4j.releaser.actions;

import java.io.PrintStream;
import java.util.Map;

import org.scm4j.releaser.ExtendedStatus;
import org.scm4j.releaser.conf.Component;

import com.google.common.base.Strings;


public class PrintStatus {
	
	private int level = 0;
	
	public void print(PrintStream ps, ExtendedStatus node) {
		ps.println(Strings.repeat("\t", level) + node);
		level++;
		for (Map.Entry<Component, ExtendedStatus> entry : node.getSubComponents().entrySet()) {
			print(ps, entry.getValue());
		}
		level--;
	}
}
