package org.scm4j.wf;

import org.scm4j.wf.conf.Coords;
import org.scm4j.wf.conf.Version;

import junit.framework.TestCase;

public class CoordsTest extends TestCase {

	public void testCoords() {
		try {
			new Coords("");
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	Coords dc(String coords) {
		return new Coords(coords);
	}

	public void testComment() {
		assertEquals("", dc("com.myproject:c1").getComment());
		assertEquals("#", dc("com.myproject:c1#").getComment());
		assertEquals("#...$ #", dc("com.myproject:c1#...$ #").getComment());
	}

	public void testExtension() {
		assertEquals("", dc("com.myproject:c1").getExtension());
		assertEquals("@", dc("com.myproject:c1@").getExtension());
		assertEquals("@ext", dc("com.myproject:c1@ext#qw").getExtension());
		assertEquals("@ext@", dc("com.myproject:c1@ext@#qw").getExtension());
	}

	public void testClassifier() {
		assertEquals("", dc("com.myproject:c1").getClassifier());
		assertEquals(":", dc("com.myproject:c1::").getClassifier());
		assertEquals(":class", dc("com.myproject:c1::class:").getClassifier());
	}

	public void testToSting() {
		assertEquals("com.myproject:c1:1.0.0", dc("com.myproject:c1:1.0.0").toString());
		assertEquals("com.myproject:  c1:1.0.0", dc("com.myproject:  c1:1.0.0").toString());
		assertEquals("   com.myproject:  c1:1.0.0", dc("   com.myproject:  c1:1.0.0").toString());
		assertEquals("com.myproject:c1:1.0.0#comment", dc("com.myproject:c1:1.0.0#comment").toString());
		assertEquals("com.myproject:c1:1.0.0@ext #comment", dc("com.myproject:c1:1.0.0@ext #comment").toString());
		assertEquals("com.myproject:c1::dfgd@ext #comment", dc("com.myproject:c1::dfgd@ext #comment").toString());
	}
	
	public void testGroupId() {
		assertEquals("com.myproject", dc("com.myproject:c1:1.0.0").getGroupId());
		assertEquals("com.myproject", dc("com.myproject:c1").getGroupId());
		assertEquals("   com.myproject", dc("   com.myproject:c1").getGroupId());
	}
	
	public void testArtifactId() {
		assertEquals("c1", dc("com.myproject:c1:1.0.0").getArtifactId());
		assertEquals("c1", dc("com.myproject:c1").getArtifactId());
		assertEquals("c1", dc("   com.myproject:c1").getArtifactId());
	}
	
	public void testVersion() {
		assertEquals(new Version("1.0.0"), dc("com.myproject:c1:1.0.0").getVersion());
		assertEquals(new Version("1.0.0"), dc("com.myproject:c1:1.0.0#comment").getVersion());
		assertEquals(new Version("1.0.0"), dc("com.myproject:c1:1.0.0@ext #comment").getVersion());
		assertEquals(new Version(""), dc("com.myproject:c1::dfgd@ext #comment").getVersion());
		assertEquals(new Version("-SNAPSHOT"), dc("com.myproject:c1:-SNAPSHOT:dfgd@ext #comment").getVersion());
	}
}
