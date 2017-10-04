package org.scm4j.releaser.conf;

import org.junit.Test;
import org.scm4j.releaser.TestEnvironment;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class MDepsFileTest {

	@Test
	public void testHasMDeps() {
		assertFalse(new MDepsFile("").hasMDeps());
		assertFalse(new MDepsFile((String) null).hasMDeps());
		assertFalse(new MDepsFile((List<Component>) null).hasMDeps());
		assertTrue(new MDepsFile(TestEnvironment.PRODUCT_UNTILL).hasMDeps());
		assertTrue(new MDepsFile(Arrays.asList(new Component(TestEnvironment.PRODUCT_UNTILL)))
				.hasMDeps());
	}

	@Test
	public void testGetMDeps() {
		assertTrue(new MDepsFile("").getMDeps().isEmpty());
		assertTrue(new MDepsFile((String) null).getMDeps().isEmpty());
		assertTrue(new MDepsFile((List<Component>) null).getMDeps().isEmpty());
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		assertTrue(new MDepsFile(TestEnvironment.PRODUCT_UNTILL).getMDeps().contains(comp));
		assertTrue(new MDepsFile(Arrays.asList(comp)).getMDeps().contains(comp));
	}

	@Test
	public void testToFileContent() {
		assertTrue(new MDepsFile("").toFileContent().isEmpty());
		assertTrue(new MDepsFile((String) null).toFileContent().isEmpty());
		assertTrue(new MDepsFile((List<Component>) null).toFileContent().isEmpty());
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		assertTrue(new MDepsFile(TestEnvironment.PRODUCT_UNTILL).toFileContent().equals(comp.toString()));
		assertTrue(new MDepsFile(Arrays.asList(comp)).toFileContent().equals(comp.toString()));
	}

	@Test
	public void testReplace() {
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		MDepsFile mdf = new MDepsFile(Arrays.asList(comp));
		Component modifiedComp = comp.clone("11.12.13");
		mdf.replaceMDep(modifiedComp);
		assertTrue(mdf.getMDeps().size() == 1);
		assertTrue(mdf.getMDeps().contains(modifiedComp));

		Component wrongComp = new Component("wrong.comp:none");
		mdf.replaceMDep(wrongComp);
		assertTrue(mdf.getMDeps().size() == 1);
		assertTrue(mdf.getMDeps().contains(modifiedComp));
	}

	@Test
	public void testFormatSavingOnReplace() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println();
		pw.println("        # my cool comment ");
		pw.println();
		pw.println("    " + TestEnvironment.PRODUCT_UNTILL + " # product comment ");
		pw.print("  ");
		String content = sw.toString();
		MDepsFile mdf = new MDepsFile(content);
		assertTrue(mdf.hasMDeps());
		List<Component> mDeps = mdf.getMDeps();
		assertTrue(mDeps.size() == 1);
		assertEquals(new Component(TestEnvironment.PRODUCT_UNTILL), mDeps.get(0));

		Component initialComp = new Component("    " + TestEnvironment.PRODUCT_UNTILL + " # product comment ");
		Component versionedComp = initialComp.clone("12.13.14");
		mdf.replaceMDep(versionedComp);

		assertEquals(content.replace(TestEnvironment.PRODUCT_UNTILL, TestEnvironment.PRODUCT_UNTILL + ":12.13.14"), mdf.toFileContent());
	}
	
	@Test
	public void testToString() {
		MDepsFile mdf = new MDepsFile("");
		assertNotNull(mdf.toString());
	}
}
