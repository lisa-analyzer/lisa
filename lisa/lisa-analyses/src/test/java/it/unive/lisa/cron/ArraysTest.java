package it.unive.lisa.cron;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import org.junit.Test;

public class ArraysTest extends AnalysisTestExecutor {

	@Test
	public void monolithTest() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				new MonolithicHeap(),
				DefaultConfiguration.defaultValueDomain(),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "arrays";
		conf.testSubDir = "monolith";
		conf.programFile = "arrays.imp";
		perform(conf);
	}

	@Test
	public void fieldInsensitiveTest() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				new PointBasedHeap(),
				DefaultConfiguration.defaultValueDomain(),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "arrays";
		conf.testSubDir = "allocations";
		conf.programFile = "arrays.imp";
		perform(conf);
	}

	@Test
	public void fieldSensitiveTest() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				new FieldSensitivePointBasedHeap(),
				DefaultConfiguration.defaultValueDomain(),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "arrays";
		conf.testSubDir = "allocations-fields";
		conf.programFile = "arrays.imp";
		perform(conf);
	}
}
