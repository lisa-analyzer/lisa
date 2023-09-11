package it.unive.lisa.cron;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import org.junit.Test;

public class PointBasedHeapTest extends AnalysisTestExecutor {

	@Test
	public void fieldInsensitivePointBasedHeapTest() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				new PointBasedHeap(),
				DefaultConfiguration.defaultValueDomain(),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "heap/point-based-heap/field-insensitive";
		conf.programFile = "program.imp";
		perform(conf);
	}

	@Test
	public void fieldSensitivePointBasedHeapTest() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				new FieldSensitivePointBasedHeap(),
				DefaultConfiguration.defaultValueDomain(),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "heap/point-based-heap/field-sensitive";
		conf.programFile = "program.imp";
		perform(conf);
	}
}
