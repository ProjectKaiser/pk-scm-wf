package org.scm4j.releaser.conf;

import org.scm4j.commons.CommentedString;
import org.scm4j.commons.coords.CoordsGradle;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class MDepsFile {

	private final List<String> lines = new ArrayList<>();

	public MDepsFile(String content) {
		if (content == null || content.isEmpty()) {
			return;
		}
		Collections.addAll(lines, content.split("\\r?\\n", -1));
	}

//	public MDepsFile (Component... comps) {
//		this(Arrays.asList(comps));
//	}
//
//	public MDepsFile (Collection<Component> mDeps) {
//		StringBuilder sb = new StringBuilder();
//		for (Component mDep : mDeps) {
//			lines.add(mDep.getCoords().toString());
//		}
//	}

	public void replaceMDep(Component newMDep) {
		ListIterator<String> it = lines.listIterator();
		String str;
		while (it.hasNext()) {
			str = it.next();
			if (new CoordsGradle(str).toString("").equals(newMDep.getCoordsNoVersion())) {
				it.set(newMDep.getCoords().toString());
				return;
			}
		}
	}

	public String toFileContent() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		for (int i = 0; i < lines.size(); i++) {
			if (i == lines.size() - 1) {
				pw.print(lines.get(i));
			} else {
				pw.println(lines.get(i));
			}
		}
		return sw.toString();
	}

	public List<Component> getMDeps(VCSRepositoryFactory repoFactory) {
		List<Component> res = new ArrayList<>();
		for (String line : lines) {
			CommentedString cs = new CommentedString(line);
			if (cs.isValuable()) {
				res.add(new Component(line, repoFactory));
			}
		}
		return res;
	}

	@Override
	public String toString() {
		return "MDepsFile [mDeps=" + lines + "]";
	}

	public boolean hasMDeps() {
		for (String line : lines) {
			CommentedString cs = new CommentedString(line);
			if (cs.isValuable()) {
				return true;
			}
		}
		return false;
	}


}
