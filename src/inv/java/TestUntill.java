import org.junit.Test;
import org.scm4j.releaser.ActionTreeBuilder;

public class TestUntill {
	
	@Test
	public void testLags() throws Exception {
		ActionTreeBuilder actionBuilder = new ActionTreeBuilder();
		actionBuilder.getActionTree("eu.untill.sdk.drivers:six-payment-eft-driver:1.0");
	}
}
