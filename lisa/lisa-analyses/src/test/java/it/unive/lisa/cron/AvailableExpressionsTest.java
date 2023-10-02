package it.unive.lisa.cron;

import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.dataflow.AvailableExpressions;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import org.junit.Test;

public class AvailableExpressionsTest extends AnalysisTestExecutor {

	@Test
	public void testAvailableExpressions() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new DefiniteDataflowDomain<>(new AvailableExpressions()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "available-expressions";
		conf.programFile = "available-expressions.imp";
		perform(conf);
	}
}