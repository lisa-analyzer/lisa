package it.unive.lisa.cron;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.memory.MonolithicMemory;
import it.unive.lisa.analysis.memory.pointbased.FieldSensitivePointBasedMemory;
import it.unive.lisa.analysis.memory.pointbased.PointBasedMemory;
import it.unive.lisa.outputs.JSONResults;
import org.junit.jupiter.api.Test;

public class ArraysTest
		extends
		IMPCronExecutor {

	@Test
	public void monolithTest() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				new MonolithicMemory(),
				DefaultConfiguration.defaultValueDomain(),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "arrays";
		conf.testSubDir = "monolith";
		conf.programFile = "arrays.imp";
		perform(conf);
	}

	@Test
	public void fieldInsensitiveTest() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				new PointBasedMemory(),
				DefaultConfiguration.defaultValueDomain(),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "arrays";
		conf.testSubDir = "allocations";
		conf.programFile = "arrays.imp";
		perform(conf);
	}

	@Test
	public void fieldSensitiveTest() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				new FieldSensitivePointBasedMemory(),
				DefaultConfiguration.defaultValueDomain(),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "arrays";
		conf.testSubDir = "allocations-fields";
		conf.programFile = "arrays.imp";
		perform(conf);
	}

}
