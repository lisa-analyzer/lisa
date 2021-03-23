package it.unive.lisa.cron.typeInference;

import org.junit.Test;

import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;

public class TypesCollectionTest extends AnalysisTestExecutor {
	@Test
	public void testTypesCollection() {
		LiSAConfiguration conf = new LiSAConfiguration().setInferTypes(true).setDumpTypeInference(true);
		perform("type-inference", "program.imp", conf);
	}
}
