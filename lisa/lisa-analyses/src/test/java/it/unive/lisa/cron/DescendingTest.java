package it.unive.lisa.cron;

import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.DescendingPhaseType;
import org.junit.Test;

public class DescendingTest extends AnalysisTestExecutor {

	@Test
	public void testIntervalDescendingWidening() {
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
	public void testIntervalDescendingMaxGlb() {
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