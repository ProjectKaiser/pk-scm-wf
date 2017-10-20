package org.scm4j.releaser.conf;

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
		for (String str: strs) {
			if (isLineValueable(str)) {
				lines.add(new Component(str));
			} else {
				lines.add(str);
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
		for (Object mDep : mDeps) {
			this.lines.add(mDep);
		}
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
