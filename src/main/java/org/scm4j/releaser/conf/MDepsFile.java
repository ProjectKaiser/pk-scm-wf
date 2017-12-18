package org.scm4j.releaser.conf;

import org.scm4j.commons.CommentedString;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class MDepsFile {

	private final List<Object> lines = new ArrayList<>();

	public MDepsFile(String content) {
		if (content == null || content.isEmpty()) {
			return;
		}
		String[] strs = content.split("\\r?\\n", -1);
		for (String str : strs) {
			if (new CommentedString(str).isValuable()) {
				lines.add(new Component(str));
			} else {
				lines.add(str);
			}
		}
	}

	public void replaceMDep(Component newMDep) {
		ListIterator<Object> it = lines.listIterator();
		Object obj;
		while (it.hasNext()) {
			obj = it.next();
			if (obj instanceof Component) {
				if (((Component) obj).getCoordsNoVersion().equals(newMDep.getCoordsNoVersion())) {
					it.set(newMDep);
					return;
				}
			}
		}
	}

	public MDepsFile(List<Component> mDeps) {
		if (mDeps == null) {
			return;
		}
		lines.addAll(mDeps);
	}

	public String toFileContent() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		for (int i = 0; i < lines.size(); i++) {
			if (i == lines.size() - 1) {
				pw.print(lines.get(i).toString());
			} else {
				pw.println(lines.get(i).toString());
			}
		}
		return sw.toString();
	}

	public List<Component> getMDeps() {
		List<Component> res = new ArrayList<>();
		for (Object obj : lines) {
			if (obj instanceof Component) {
				res.add((Component) obj);
			}
		}
		return res;
	}

	@Override
	public String toString() {
		return "MDepsFile [mDeps=" + lines + "]";
	}

	public boolean hasMDeps() {
		for (Object obj : lines) {
			if (obj instanceof Component) {
				return true;
			}
		}
		return false;
	}
}
