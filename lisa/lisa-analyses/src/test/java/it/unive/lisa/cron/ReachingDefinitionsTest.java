package it.unive.lisa.cron;

import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.dataflow.PossibleForwardDataflowDomain;
import it.unive.lisa.analysis.dataflow.ReachingDefinitions;
import org.junit.Test;

public class ReachingDefinitionsTest extends AnalysisTestExecutor {

	@Test
	public void testReachingDefinitions() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new PossibleForwardDataflowDomain<>(new ReachingDefinitions()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "reaching-definitions";
		conf.programFile = "reaching-definitions.imp";
		perform(conf);
	}
}