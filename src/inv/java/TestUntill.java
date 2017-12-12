import org.junit.Test;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.conf.Component;

public class TestUntill {
	
	@Test
	public void testLags() throws Exception {
		SCMReleaser releaser = new SCMReleaser();
		releaser.getActionTree(new Component("eu.untill.sdk.drivers:six-payment-eft-driver:1.0"));
	}
}
