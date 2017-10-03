package org.scm4j.releaser.conf;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class MDepsFile {
	
	private List<Object> mDeps = new ArrayList<>();
	
	public MDepsFile(String content) {
		if (content == null || content.isEmpty()) {
			return;
		}
		String[] strs = content.split("\\r?\\n", -1);
		for (String str: strs) {
			if (isLineValueable(str)) {
				mDeps.add(new Component(str));
			} else {
				mDeps.add(str);
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
		int indexToReplace = -1;
		for (Object obj : mDeps) {
			if (obj instanceof Component) {
				if (((Component) obj).getName().equals(newMDep.getName())) {
					indexToReplace = mDeps.lastIndexOf(obj);
					break;
				}
			}
		}
		
		if (indexToReplace > -1) {
			mDeps.set(indexToReplace, newMDep);
		}
	}

	public MDepsFile(List<Component> mDeps) {
		if (mDeps == null) {
			return;
		}
		for (Object mDep : mDeps) {
			this.mDeps.add(mDep);
		}
	}

	public String toFileContent() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		for (int i = 0; i < mDeps.size(); i++) {
//			if (i == mDeps.size() - 1) {
//				pw.print(mDeps.get(i).toString());
//			} else {
				pw.println(mDeps.get(i).toString());
//			}
			
		}
		return sw.toString();
	}
	
	public List<Component> getMDeps() {
		List<Component> res = new ArrayList<>();
		for (Object obj : mDeps) {
			if (obj instanceof Component) {
				res.add((Component) obj);
			}
		}
		return res;
	}

	@Override
	public String toString() {
		return "MDepsFile [mDeps=" + mDeps + "]";
	}

	public boolean hasMDeps() {
		for (Object obj : mDeps) {
			if (obj instanceof Component) {
				return true;
			}
		}
		return false;
	}
	
}
