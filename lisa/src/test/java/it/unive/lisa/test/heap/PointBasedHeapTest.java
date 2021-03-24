package it.unive.lisa.test.heap;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.impl.numeric.Interval;
import it.unive.lisa.test.AnalysisTest;
import org.junit.Test;

public class PointBasedHeapTest extends AnalysisTest {

	@Test
	public void fieldInensitivePointBasedHeapTest() throws AnalysisSetupException {
		perform("heap/point-based-heap/field-insensitive", "program.imp", false, false,
				getDefaultFor(AbstractState.class, new PointBasedHeap(), new Interval()));
	}

	@Test
	public void fieldSensitivePointBasedHeapTest() throws AnalysisSetupException {
		perform("heap/point-based-heap/field-sensitive", "program.imp", false, false,
				getDefaultFor(AbstractState.class, new PointBasedHeap(true), new Interval()));
	}
}
