package it.unive.lisa.cron;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.traces.TracePartitioning;
import org.junit.Test;

public class TracePartitioningTest extends IMPCronExecutor {

	@Test
	public void testTracePartitioning() {
		CronConfiguration conf = new CronConfiguration();
		conf.analysis = new TracePartitioning<>(3, 5, DefaultConfiguration.defaultAbstractDomain());
		conf.serializeResults = true;
		conf.testDir = "traces";
		conf.programFile = "traces.imp";
		perform(conf);
	}

}
