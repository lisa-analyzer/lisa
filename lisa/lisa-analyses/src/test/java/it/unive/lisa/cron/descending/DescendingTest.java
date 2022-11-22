package it.unive.lisa.cron.descending;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.LiSAConfiguration.DescendingPhaseType;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.types.InferredTypes;
import org.junit.Test;

public class DescendingTest extends AnalysisTestExecutor {

	@Test
	public void testIntervalDescendingWidening() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Interval(),
				new TypeEnvironment<>(new InferredTypes()));
		conf.descendingPhaseType = DescendingPhaseType.NARROWING;
		perform("descending-widening", "program.imp", conf);
	}

	@Test
	public void testIntervalDescendingMaxGlb() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Interval(),
				new TypeEnvironment<>(new InferredTypes()));
		conf.descendingPhaseType = DescendingPhaseType.GLB;
		conf.descendingGlbThreshold = 5;
		perform("descending-maxglb", "program.imp", conf);
	}
}