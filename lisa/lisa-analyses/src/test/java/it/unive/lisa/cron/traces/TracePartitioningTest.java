package it.unive.lisa.cron.traces;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.LiSAFactory;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.traces.TracePartitioning;
import it.unive.lisa.analysis.value.TypeDomain;
import org.junit.Test;

public class TracePartitioningTest extends AnalysisTestExecutor {

	@Test
	public void testTracePartitioning() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.abstractState = new TracePartitioning<>(
				new SimpleAbstractState<>(
						LiSAFactory.getDefaultFor(HeapDomain.class),
						new ValueEnvironment<>(new Interval()),
						LiSAFactory.getDefaultFor(TypeDomain.class)));
		conf.serializeResults = true;
		int prev = TracePartitioning.MAX_LOOP_ITERATIONS;
		TracePartitioning.MAX_LOOP_ITERATIONS = 3;
		conf.testDir = "traces";
		conf.programFile = "traces.imp";
		perform(conf);
		TracePartitioning.MAX_LOOP_ITERATIONS = prev;
	}
}
