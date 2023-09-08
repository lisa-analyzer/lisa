package it.unive.lisa.cron.descending;

import org.junit.Test;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.DescendingPhaseType;

public class DescendingTest extends AnalysisTestExecutor {

	@Test
	public void testIntervalDescendingWidening() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				DefaultConfiguration.defaultValueDomain(),
				DefaultConfiguration.defaultTypeDomain());
		conf.descendingPhaseType = DescendingPhaseType.NARROWING;
		conf.testDir = "descending-widening";
		conf.programFile = "program.imp";
		perform(conf);
	}

	@Test
	public void testIntervalDescendingMaxGlb() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				DefaultConfiguration.defaultValueDomain(),
				DefaultConfiguration.defaultTypeDomain());
		conf.descendingPhaseType = DescendingPhaseType.GLB;
		conf.glbThreshold = 5;
		conf.testDir = "descending-maxglb";
		conf.programFile = "program.imp";
		perform(conf);
	}
}