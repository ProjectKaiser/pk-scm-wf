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
	public void testComponentReplaceAndFormatSaving() {
		Component comp1 = new Component("eu.untill:TPAPIJavaProxy::tests # tpapiTests");
		Component comp2 = new Component("eu.untill:TPAPIJavaProxy: #tpapiTests");
		Component comp3 = new Component("          eu.untill.utils:UntillWD:@zip # thirdParty");
		Component comp4 = new Component("eu.untill.utils:UntillWD::sources@zip # thirdPartyLib");
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println();
		pw.println("        # my cool comment ");
		pw.println();
		pw.println(comp1.toString());
		pw.println(comp2.toString());
		pw.println(comp3.toString());
		pw.println(comp4.toString());
		pw.print("  ");
		
		MDepsFile mdf = new MDepsFile(sw.toString());
		assertTrue(mdf.getMDeps().size() == 4);
		assertTrue(mdf.getMDeps().containsAll(Arrays.asList(comp1, comp2, comp3, comp4)));
		assertEquals(sw.toString(), mdf.toFileContent());
		
		Component comp1Versioned = comp1.clone("12.13");
		Component comp2Versioned = comp2.clone("12.14");
		Component comp3Versioned = comp3.clone("12.15");
		Component comp4Versioned = comp4.clone("12.16");

		mdf.replaceMDep(comp1Versioned);
		mdf.replaceMDep(comp2Versioned);
		mdf.replaceMDep(comp3Versioned);
		mdf.replaceMDep(comp4Versioned);
		
		assertTrue(mdf.getMDeps().size() == 4);
		assertTrue(mdf.getMDeps().containsAll(Arrays.asList(comp1Versioned, comp2Versioned, comp3Versioned, comp4Versioned)));
		assertEquals(sw.toString()
				.replace(comp1.toString(), comp1Versioned.toString())
				.replace(comp2.toString(), comp2Versioned.toString())
				.replace(comp3.toString(), comp3Versioned.toString())
				.replace(comp4.toString(), comp4Versioned.toString()), mdf.toFileContent());
	}
	
	@Test
	public void testToString() {
		MDepsFile mdf = new MDepsFile("");
		assertNotNull(mdf.toString());
	}
}
