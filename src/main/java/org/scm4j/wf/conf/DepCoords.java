package org.scm4j.wf.conf;

public class DepCoords {

	protected String nameStr = "";
	private String commentStr = "";
	private final String extStr;
	private String groupStr = "";
	private String verStr = "";
	private String classStr = "";
	private Version version;
	
	public String getComment(){
		return commentStr;
	}

	public DepCoords(String sourceString) {
		String str = sourceString;
		
		// Comment
		{
			Integer pos = sourceString.indexOf("#");
			if (pos > 0) {
				commentStr = str.substring(pos);
				str = str.substring(0, pos);
			}
		}
		
		// Extension
		{
			Integer pos = sourceString.indexOf("@");
			if (pos > 0) {
				extStr  = str.substring(pos);
				str = str.substring(0, pos);
			} else {
				extStr = "";				
			}
		}
		
		String[] strs = str.split(":");
		if (strs.length < 2) {
			throw new IllegalArgumentException("wrong mdep coord: " + sourceString);
		}
		
		groupStr = strs[0];
		nameStr = strs[1];
		
		if( strs.length > 2){
			verStr = strs[2];
		} else {
			verStr = "0.1.0";			
		}
		if( strs.length > 3){
			classStr = ":" + strs[3];
		}
		
		version = new Version(verStr);
	}
	
	public Version getVersion() {
		return new Version(verStr);
	}
	
	@Override
	public String toString() {
		return getName() + ":" + version.toString() + classStr + extStr + commentStr;
	}
	
	public String getName() {
		return groupStr + ":" + nameStr;
	}
	
	public void setVersion(Version version) {
		this.version = version;
	}

	public String getExtension() {
		return extStr;
	}
	

}
