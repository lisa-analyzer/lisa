package it.unive.lisa.cron;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.memory.TypeBasedMemory;
import it.unive.lisa.analysis.memory.pointbased.FieldSensitivePointBasedMemory;
import it.unive.lisa.analysis.memory.pointbased.PointBasedMemory;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.outputs.JSONResults;
import org.junit.jupiter.api.Test;

public class MemoryAbstractionsTest
		extends
		IMPCronExecutor {

	@Test
	public void testTypeBasedMemory() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				new TypeBasedMemory(),
				DefaultConfiguration.defaultValueDomain(),
				DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(-1);
		conf.testDir = "memory";
		conf.testSubDir = "types";
		conf.programFile = "memory-type.imp";
		conf.allMethods = true;
		perform(conf);
	}

	@Test
	public void fieldInsensitivePointBasedMemoryTest() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				new PointBasedMemory(),
				DefaultConfiguration.defaultValueDomain(),
				DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(-1);
		conf.testDir = "memory";
		conf.testSubDir = "pp";
		conf.programFile = "memory-pp.imp";
		conf.allMethods = true;
		perform(conf);
	}

	@Test
	public void FieldSensitivePointBasedMemoryTest() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				new FieldSensitivePointBasedMemory(),
				DefaultConfiguration.defaultValueDomain(),
				DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(-1);
		conf.testDir = "memory";
		conf.testSubDir = "pp-field";
		conf.programFile = "memory-pp-field.imp";
		conf.allMethods = true;
		perform(conf);
	}

	@Test
	public void fieldInsensitiveGCTest() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				new PointBasedMemory(),
				DefaultConfiguration.defaultValueDomain(),
				DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(-1);
		conf.testDir = "memory";
		conf.testSubDir = "pp-gc";
		conf.programFile = "gc.imp";
		perform(conf);
	}

	@Test
	public void fieldSensitiveGCTest() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				new FieldSensitivePointBasedMemory(),
				DefaultConfiguration.defaultValueDomain(),
				DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(-1);
		conf.testDir = "memory";
		conf.testSubDir = "pp-field-gc";
		conf.programFile = "gc.imp";
		perform(conf);
	}

}
