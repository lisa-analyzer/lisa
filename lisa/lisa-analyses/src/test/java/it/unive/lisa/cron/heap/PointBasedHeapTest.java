package it.unive.lisa.cron.heap;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import org.junit.Test;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.value.TypeDomain;

public class PointBasedHeapTest extends AnalysisTestExecutor {

	@Test
	public void fieldInsensitivePointBasedHeapTest() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				new PointBasedHeap(),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.testDir = "heap/point-based-heap/field-insensitive";
		conf.programFile = "program.imp";
		perform(conf);
	}

	@Test
	public void fieldSensitivePointBasedHeapTest() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				new FieldSensitivePointBasedHeap(),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.testDir = "heap/point-based-heap/field-sensitive";
		conf.programFile = "program.imp";
		perform(conf);
	}
}
