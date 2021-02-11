package it.unive.lisa.test.typeInference;

import org.junit.Test;

import it.unive.lisa.test.AnalysisTest;

public class TypesCollectionTest extends AnalysisTest {
	@Test
	public void testTypesCollection() {
		perform("type-inference", "program.imp", true, true, null);
	}
}
