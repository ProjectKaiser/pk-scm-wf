package org.scm4j.releaser.conf;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class MDepsFile {
	
	private List<Component> mDeps = new ArrayList<>();
	
	public MDepsFile(String content) {
		String[] strs = StringUtils.split(content, "\r\n");
		for (String str: strs) {
			if (isLineValueable(str)) {
				mDeps.add(new Component(str));
			}
		}
	}
	
	private boolean isLineValueable(String str) {
		Integer pos = str.indexOf("#");
		if (pos >= 0) {
			str = str.substring(0, pos);
		}
		return !str.trim().isEmpty();
	}

	public MDepsFile(List<Component> mDeps) {
		this.mDeps = mDeps;
	}

	public String toFileContent() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		for (Component mDep : mDeps) {
			pw.println(mDep.toString());
		}
		return sw.toString();
	}
	
	public List<Component> getMDeps() {
		return mDeps;
	}

	@Override
	public String toString() {
		return "MDepsFile [mDeps=" + mDeps + "]";
	}
	
}
