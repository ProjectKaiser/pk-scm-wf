package org.scm4j.wf.conf;

public class DepCoords {

	protected String name;
	private String tail;
	private String preName;
	private String comment;
	private String group;
	protected Version ver;
	
	public DepCoords() {
		
	}

	public DepCoords(String sourceString) {
		String str = sourceString;

		// комментарий
		Integer commentPos = sourceString.indexOf("#");
		if (commentPos > 0) {
			comment = "#" + str.substring(commentPos);
			str = str.substring(0, commentPos - 1);
		}
		
		
		String[] strs = str.split(":");
		if (strs.length < 2) {
			throw new IllegalArgumentException("wrong mdep coord: " + sourceString);
		}
		
		group = strs[0].trim();
		StringBuilder sb = new StringBuilder();
		for (Integer i = 0; i <= group.length(); i++) {
			if (group.charAt(i) != strs[0].charAt(0)) {
				sb.append(group.charAt(i));
			} else {
				break;
			}
		}
		preName = sb.toString();
		
		name = strs[1];
		
		if (strs.length == 2) {
			return;
		}
		
		// если после имени нет версии, только классификатор
		if (strs[2].indexOf(".") < 0) {
			tail = strs[2];
			return;
		}
		
		// все что после версии
		if (strs.length > 3) {
			sb = new StringBuilder();
			String prefix = ":";
			for (Integer i = 3; i <= strs.length - 1; i++) {
				sb.append(prefix);
				sb.append(strs[i]);
			}
			tail = sb.toString();
		}
		
		// strs[2] - версия
		ver = new Version(strs[2]);
	}
	
	@Override
	public String toString() {
		return getName() + ":" + getVersion().toString();
	}
	
	public Version getVersion() {
		return ver;
	}
	
	public String getMDepsString() {
		return preName + toString() + tail + comment;
	}
	
	public String getName() {
		return group + ":" + name;
	}
	
	public void setVersion(Version ver) {
		this.ver = ver;
	}
	

}
