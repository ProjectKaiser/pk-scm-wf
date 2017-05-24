package org.scm4j.progress;

import junit.framework.TestCase;

public class ProgressReporterConsoleTest extends TestCase {

	public void testMultiline(){
		
		System.out.println("***Multiline:");
		ProgressConsole rpc1 = new ProgressConsole("Progress 1", false);
		rpc1.reportStatus("Status1.1");
		rpc1.reportStatus("Status1.2");
		{
			IProgress rp2 = rpc1.createNestedProgress("Progress 2");
			rp2.reportStatus("Status2.1");
			rp2.reportStatus("Status2.2");
			{
				IProgress rp3 = rp2.createNestedProgress("Progress 3");
				rp3.reportStatus("Status3.1");
				rp3.reportStatus("Status3.2");
				rp3.close();
			}
			rp2.close();
		}
		rpc1.reportStatus("Status1.3");
		rpc1.reportStatus("Status1.4");
		rpc1.close();
	}

}
