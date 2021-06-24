package it.unive.lisa.cron.heap;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import org.junit.Test;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.impl.heap.TypeBasedHeap;
import it.unive.lisa.analysis.impl.numeric.Interval;

public class TypeBasedHeapTest extends AnalysisTestExecutor {

	@Test
	public void testTypeBasedHeap() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration().setDumpAnalysis(true).setInferTypes(true)
				.setAbstractState(getDefaultFor(AbstractState.class, new TypeBasedHeap(), new Interval()));
		perform("heap/type-based-heap", "program.imp", conf);
	}
}
