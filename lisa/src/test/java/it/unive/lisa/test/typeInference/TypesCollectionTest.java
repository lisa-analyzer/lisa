package it.unive.lisa.test.typeInference;

import it.unive.lisa.test.AnalysisTest;
import org.junit.Test;

public class TypesCollectionTest extends AnalysisTest {
	@Test
	public void testTypesCollection() {
		perform("type-inference", "program.imp", true, true, null);
	}
}
