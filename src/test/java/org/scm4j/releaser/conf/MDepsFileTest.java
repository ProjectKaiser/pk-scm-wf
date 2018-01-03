package org.scm4j.releaser.conf;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scm4j.releaser.testutils.TestEnvironment;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import static org.junit.Assert.*;

public class MDepsFileTest {

	private static final TestEnvironment env = new TestEnvironment();
	
	@BeforeClass
	public static void setUp() throws Exception {
		env.generateTestEnvironmentNoVCS();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		env.close();
	}

	private MDepsFile getMDF(String content) {
		return new MDepsFile(content);
	}

	@Test
	public void testHasMDeps() {
		assertFalse(getMDF("").hasMDeps());
		assertFalse(getMDF("# non-component").hasMDeps());
		assertFalse(getMDF(null).hasMDeps());
		assertTrue(getMDF(TestEnvironment.PRODUCT_UNTILL).hasMDeps());
	}

	@Test
	public void testGetMDeps() {
		assertTrue(getMDF("").getMDeps().isEmpty());
		assertTrue(getMDF(null).getMDeps().isEmpty());
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		assertTrue(getMDF(TestEnvironment.PRODUCT_UNTILL).getMDeps().contains(comp));
		assertTrue(getMDF(comp.getCoords().toString()).getMDeps().contains(comp));
	}

	@Test
	public void testToFileContent() {
		assertTrue(getMDF("").toFileContent().isEmpty());
		assertTrue(getMDF(null).toFileContent().isEmpty());
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		assertTrue(getMDF(TestEnvironment.PRODUCT_UNTILL).toFileContent().equals(comp.toString()));
		assertTrue(getMDF(comp.getCoords().toString()).toFileContent().equals(comp.toString()));
	}

	@Test
	public void testReplace() {
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		MDepsFile mdf = getMDF(comp.getCoords().toString());
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
		
		MDepsFile mdf = getMDF(sw.toString());
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
	public void coverToString() {
		MDepsFile mdf = getMDF("");
		mdf.toString();
	}
}
