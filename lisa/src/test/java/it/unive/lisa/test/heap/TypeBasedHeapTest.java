package it.unive.lisa.test.heap;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.TypeBasedHeap;
import it.unive.lisa.analysis.impl.numeric.Interval;
import it.unive.lisa.test.AnalysisTest;
import org.junit.Test;

public class TypeBasedHeapTest extends AnalysisTest {

	@Test
	public void testSign() throws AnalysisSetupException {
		perform("type-based-heap", "program.imp", false, false,
				getDefaultFor(AbstractState.class, new TypeBasedHeap(), new Interval()));
	}
}
