package it.unive.lisa.cron.typeInference;

import org.junit.Test;

import it.unive.lisa.AnalysisTestExecutor;

public class TypesCollectionTest extends AnalysisTestExecutor {
	@Test
	public void testTypesCollection() {
		perform("type-inference", "program.imp", true, true, null);
	}
}
