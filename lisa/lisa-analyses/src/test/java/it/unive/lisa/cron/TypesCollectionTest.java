package it.unive.lisa.cron;

import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import org.junit.Test;

public class TypesCollectionTest
		extends
		AnalysisTestExecutor {

	@Test
	public void testTypesCollection() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration
				.simpleState(
						DefaultConfiguration.defaultHeapDomain(),
						DefaultConfiguration.defaultValueDomain(),
						new InferredTypes());
		conf.testDir = "type-inference";
		conf.testSubDir = "basic";
		conf.programFile = "inference.imp";
		perform(conf);
	}

	@Test
	public void testTypesCollectionOnObjects() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration
				.simpleState(
						new FieldSensitivePointBasedHeap(),
						DefaultConfiguration.defaultValueDomain(),
						new InferredTypes());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.testDir = "type-inference";
		conf.testSubDir = "objects";
		conf.programFile = "objects.imp";
		perform(conf);
	}

}
