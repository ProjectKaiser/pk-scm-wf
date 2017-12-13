import org.junit.Test;
import org.scm4j.releaser.ActionTreeBuilder;
import org.scm4j.releaser.conf.Component;

public class TestUntill {
	
	@Test
	public void testLags() throws Exception {
		ActionTreeBuilder actionBuilder = new ActionTreeBuilder();
		actionBuilder.getActionTree(new Component("eu.untill.sdk.drivers:six-payment-eft-driver:1.0"));
	}
}
