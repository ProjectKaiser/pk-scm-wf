package org.scm4j.wf.conf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class MDepsFile {
	
	private List<Component> mDeps = new ArrayList<>();
	
	public MDepsFile(String content, VCSRepositories repos) {
		BufferedReader br = new BufferedReader(new StringReader(content));
		try {
			String str = br.readLine();
			while (str != null) {
				if (isLineValueable(str)) {
					Component dep = new Component(str, repos);
					mDeps.add(dep);

				}
				str = br.readLine();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private boolean isLineValueable(String str) {
		Integer pos = str.indexOf("#");
		if (pos >= 0) {
			str = str.substring(0, pos);
		}
		return !str.trim().isEmpty();
	}

	public MDepsFile(String content, VCSRepository vcsRepo) {
		BufferedReader br = new BufferedReader(new StringReader(content));
		try {
			String str = br.readLine();
			while (str != null) {
				Component dep = new Component(str, vcsRepo);
				mDeps.add(dep);
				str = br.readLine();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
