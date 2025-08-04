package it.unive.lisa.cron;

import org.junit.Test;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.heap.TypeBasedHeap;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;

public class MemoryAbstractionsTest
		extends
		IMPCronExecutor {

	@Test
	public void testTypeBasedHeap() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration
				.simpleState(
						new TypeBasedHeap(),
						DefaultConfiguration.defaultValueDomain(),
						DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.testDir = "heap";
		conf.testSubDir = "types";
		conf.programFile = "heap-type.imp";
		conf.allMethods = true;
		perform(conf);
	}

	@Test
	public void fieldInsensitivePointBasedHeapTest() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration
				.simpleState(
						new PointBasedHeap(),
						DefaultConfiguration.defaultValueDomain(),
						DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.testDir = "heap";
		conf.testSubDir = "pp";
		conf.programFile = "heap-pp.imp";
		conf.allMethods = true;
		perform(conf);
	}

	@Test
	public void fieldSensitivePointBasedHeapTest() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration
				.simpleState(
						new FieldSensitivePointBasedHeap(),
						DefaultConfiguration.defaultValueDomain(),
						DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.testDir = "heap";
		conf.testSubDir = "pp-field";
		conf.programFile = "heap-pp-field.imp";
		conf.allMethods = true;
		perform(conf);
	}

	@Test
	public void fieldInsensitiveGCTest() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration
				.simpleState(
						new PointBasedHeap(),
						DefaultConfiguration.defaultValueDomain(),
						DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.testDir = "heap";
		conf.testSubDir = "pp-gc";
		conf.programFile = "gc.imp";
		perform(conf);
	}

	@Test
	public void fieldSensitiveGCTest() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration
				.simpleState(
						new FieldSensitivePointBasedHeap(),
						DefaultConfiguration.defaultValueDomain(),
						DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.testDir = "heap";
		conf.testSubDir = "pp-field-gc";
		conf.programFile = "gc.imp";
		perform(conf);
	}

}
