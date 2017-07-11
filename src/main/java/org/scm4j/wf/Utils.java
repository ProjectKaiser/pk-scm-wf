package org.scm4j.wf;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class Utils {
	
	public static String stringsToString(List<String> strings) {
		try (StringWriter sw = new StringWriter();
			 PrintWriter pw = new PrintWriter(sw)) {
			for (String str : strings) {
				pw.println(str);
			}
			return sw.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
