package it.unive.lisa.cron;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import org.junit.Test;

public class TypesCollectionTest extends AnalysisTestExecutor {
	@Test
	public void testTypesCollection() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				DefaultConfiguration.defaultValueDomain(),
				new TypeEnvironment<>(new InferredTypes()));
		conf.testDir = "type-inference";
		conf.programFile = "program.imp";
		perform(conf);
	}
}
