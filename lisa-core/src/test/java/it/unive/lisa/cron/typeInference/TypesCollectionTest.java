package it.unive.lisa.cron.typeInference;

import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import org.junit.Test;

public class TypesCollectionTest extends AnalysisTestExecutor {
	@Test
	public void testTypesCollection() {
		LiSAConfiguration conf = new LiSAConfiguration().setInferTypes(true).setDumpTypeInference(true);
		perform("type-inference", "program.imp", conf);
	}
}
