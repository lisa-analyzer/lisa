package it.unive.lisa.cron;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.traces.TracePartitioning;
import it.unive.lisa.outputs.JSONResults;
import org.junit.jupiter.api.Test;

public class TracePartitioningTest
		extends
		IMPCronExecutor {

	@Test
	public void testTracePartitioning() {
		CronConfiguration conf = new CronConfiguration();
		conf.analysis = new TracePartitioning<>(3, 5, DefaultConfiguration.defaultAbstractDomain());
		conf.outputs.add(new JSONResults<>());
		conf.testDir = "traces";
		conf.programFile = "traces.imp";
		perform(conf);
	}

}
