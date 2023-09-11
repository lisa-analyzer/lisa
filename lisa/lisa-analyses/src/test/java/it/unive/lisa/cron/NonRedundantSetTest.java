package it.unive.lisa.cron;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.nonRedundantSet.NonRedundantPowersetOfInterval;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.conf.LiSAConfiguration.DescendingPhaseType;
import org.junit.Test;

public class NonRedundantSetTest extends AnalysisTestExecutor {

	@Test
	public void testNonRedundantSetOfInterval() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new ValueEnvironment<>(new NonRedundantPowersetOfInterval()),
				DefaultConfiguration.defaultTypeDomain());
		conf.descendingPhaseType = DescendingPhaseType.GLB;
		conf.glbThreshold = 5;
		conf.testDir = "non-redundant-set-interval";
		conf.programFile = "program.imp";
		// there seem to be one less round of redundancy removal
		// that avoid compacting two elements into a single one when running an
		// optimized analysis. the result is still sound and more precice
		// however.
		conf.compareWithOptimization = false;
		perform(conf);
	}
}
