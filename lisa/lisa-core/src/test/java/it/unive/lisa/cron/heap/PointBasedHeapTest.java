package it.unive.lisa.cron.heap;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.value.TypeDomain;
import org.junit.Test;

public class PointBasedHeapTest extends AnalysisTestExecutor {

	@Test
	public void fieldInsensitivePointBasedHeapTest() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				new PointBasedHeap(),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		perform("heap/point-based-heap/field-insensitive", "program.imp", conf);
	}

	@Test
	public void fieldSensitivePointBasedHeapTest() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				new FieldSensitivePointBasedHeap(),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		perform("heap/point-based-heap/field-sensitive", "program.imp", conf);
	}
}
