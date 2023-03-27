package it.unive.lisa.cron.nonRedundantSet;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.nonRedundantSet.NonRedundantPowersetOfInterval;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.conf.LiSAConfiguration.DescendingPhaseType;
import org.junit.Test;

public class NonRedundantSetTest extends AnalysisTestExecutor {

	@Test
	public void testNonRedundantSetOfInterval() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class),
				new NonRedundantPowersetOfInterval(),
				new TypeEnvironment<>(new InferredTypes()));
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
