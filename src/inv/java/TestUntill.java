import org.junit.Test;
import org.scm4j.releaser.ActionTreeBuilder;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.ExtendedStatus;
import org.scm4j.releaser.ExtendedStatusBuilder;
import org.scm4j.releaser.conf.Component;

public class TestUntill {
	
	@Test
	public void testLags() throws Exception {
		ExtendedStatusBuilder statusBuilder = new ExtendedStatusBuilder();
		ExtendedStatus node = statusBuilder.getAndCacheMinorStatus(new Component("eu.untill.sdk.drivers:six-payment-eft-driver:1.0"));
		ActionTreeBuilder actionBuilder = new ActionTreeBuilder();
		actionBuilder.getActionTreeFull(node, new CachedStatuses());
	}
}
