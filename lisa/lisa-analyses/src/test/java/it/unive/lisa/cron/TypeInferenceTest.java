package it.unive.lisa.cron;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.analysis.types.StaticTypes;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import org.junit.Test;

public class TypeInferenceTest extends IMPCronExecutor {

	@Test
	public void testInferredTypesCollection() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration.simpleState(
			DefaultConfiguration.defaultHeapDomain(),
			DefaultConfiguration.defaultValueDomain(),
			new InferredTypes());
		conf.testDir = "type-inference";
		conf.testSubDir = "inferred-basic";
		conf.programFile = "inference.imp";
		perform(conf);
	}

	@Test
	public void testInferredCasts() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration.simpleState(
			DefaultConfiguration.defaultHeapDomain(),
			DefaultConfiguration.defaultValueDomain(),
			new InferredTypes());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.testDir = "type-inference";
		conf.testSubDir = "inferred-casts";
		conf.programFile = "casts.imp";
		perform(conf);
	}

	@Test
	public void testInferredTypesCollectionOnObjects() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration.simpleState(
			new FieldSensitivePointBasedHeap(),
			DefaultConfiguration.defaultValueDomain(),
			new InferredTypes());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.testDir = "type-inference";
		conf.testSubDir = "inferred-objects";
		conf.programFile = "objects.imp";
		perform(conf);
	}

	@Test
	public void testStaticTypesCollection() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration.simpleState(
			DefaultConfiguration.defaultHeapDomain(),
			DefaultConfiguration.defaultValueDomain(),
			new StaticTypes());
		conf.testDir = "type-inference";
		conf.testSubDir = "static-basic";
		conf.programFile = "inference.imp";
		perform(conf);
	}

	@Test
	public void testStaticCasts() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration.simpleState(
			DefaultConfiguration.defaultHeapDomain(),
			DefaultConfiguration.defaultValueDomain(),
			new StaticTypes());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.testDir = "type-inference";
		conf.testSubDir = "static-casts";
		conf.programFile = "casts.imp";
		perform(conf);
	}

	@Test
	public void testStaticTypesCollectionOnObjects() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration.simpleState(
			new FieldSensitivePointBasedHeap(),
			DefaultConfiguration.defaultValueDomain(),
			new StaticTypes());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.testDir = "type-inference";
		conf.testSubDir = "static-objects";
		conf.programFile = "objects.imp";
		perform(conf);
	}

}
