package it.unive.lisa.test.heap;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.PointBasedHeap;
import it.unive.lisa.analysis.impl.numeric.Interval;
import it.unive.lisa.test.AnalysisTest;
import org.junit.Test;

public class PointBasedHeapTest extends AnalysisTest {

	@Test
	public void pointBasedHeapTest() throws AnalysisSetupException {
		perform("heap/point-based-heap", "program.imp", false, false,
				getDefaultFor(AbstractState.class, new PointBasedHeap(), new Interval()));
	}
}
