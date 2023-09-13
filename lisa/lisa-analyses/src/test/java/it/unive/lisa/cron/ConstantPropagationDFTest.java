package it.unive.lisa.cron;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.dataflow.ConstantPropagation;
import it.unive.lisa.analysis.dataflow.DefiniteForwardDataflowDomain;
import org.junit.Test;

public class ConstantPropagationDFTest extends AnalysisTestExecutor {

	@Test
	public void testConstantPropagation() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new DefiniteForwardDataflowDomain<>(new ConstantPropagation()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "constant-propagation-df";
		conf.programFile = "constant-propagation.imp";
		perform(conf);
	}
}