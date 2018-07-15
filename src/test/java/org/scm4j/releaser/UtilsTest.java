package org.scm4j.releaser;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.function.Supplier;

import org.junit.Test;
import org.scm4j.releaser.conf.Component;

public class UtilsTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testReportDurationNoIProgress() {
		@SuppressWarnings("rawtypes")
		Supplier mockedSup = mock(Supplier.class);
		Utils.reportDuration(mockedSup, "test", new Component("test:test"), null);
		verify(mockedSup).get();
	}
}
