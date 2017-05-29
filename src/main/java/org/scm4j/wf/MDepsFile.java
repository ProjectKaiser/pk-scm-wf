package org.scm4j.wf;

import java.util.ArrayList;
import java.util.List;

public class MDepsFile {
	
	public static List<String> fromFileContent(String content) {
		String[] lines = content.split(VerFile.SEP);
		List<String> res = new ArrayList<>();
		for (String str : lines) {
			if (str.trim().startsWith(VerFile.COMMENT_PREFIX)) {
				continue;
			}
			res.add(str);
		}
		return res;
	}
	
	public static String toFileContent(List<String> mDeps) {
		StringBuilder res = new StringBuilder();
		for (String str : mDeps) {
			res.append(str);
			res.append(VerFile.SEP);
		}
		return res.toString();
	}

}
