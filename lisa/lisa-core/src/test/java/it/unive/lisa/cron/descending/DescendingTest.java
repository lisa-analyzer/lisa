package it.unive.lisa.cron.descending;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.interprocedural.InterproceduralAnalysis.DescendingPhaseType;
import org.junit.Test;

public class DescendingTest extends AnalysisTestExecutor {

	@Test
	public void testIntervalDescending() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Interval(),
				new TypeEnvironment<>(new InferredTypes()));
		conf.descendingPhaseType = DescendingPhaseType.WIDENING;
		conf.descendingGlbThreshold = 2;
		perform("descending", "program.imp", conf);
	}
}