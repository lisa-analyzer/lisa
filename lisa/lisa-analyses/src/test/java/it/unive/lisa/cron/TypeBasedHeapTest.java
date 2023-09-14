package it.unive.lisa.cron;

import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.heap.TypeBasedHeap;
import org.junit.Test;

public class TypeBasedHeapTest extends AnalysisTestExecutor {

	@Test
	public void testTypeBasedHeap() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				new TypeBasedHeap(),
				DefaultConfiguration.defaultValueDomain(),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "heap/type-based-heap";
		conf.programFile = "program.imp";
		perform(conf);
	}
}
