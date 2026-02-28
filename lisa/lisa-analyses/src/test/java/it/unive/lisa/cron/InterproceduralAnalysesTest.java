package it.unive.lisa.cron;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.numeric.IntegerConstantPropagation;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.interprocedural.ModularWorstCaseAnalysis;
import it.unive.lisa.interprocedural.callgraph.CHACallGraph;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.inlining.InliningAnalysis;
import it.unive.lisa.outputs.JSONCallGraph;
import it.unive.lisa.outputs.JSONResults;
import org.junit.jupiter.api.Test;

public class InterproceduralAnalysesTest
		extends
		IMPCronExecutor {

	@Test
	public void testWorstCaseCHA() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new Sign(),
				DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ModularWorstCaseAnalysis<>();
		conf.callGraph = new CHACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "modular-cha";
		conf.programFile = "modular.imp";
		perform(conf);
	}

	@Test
	public void testWorstCaseRTA() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new Sign(),
				DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ModularWorstCaseAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "modular-rta";
		conf.programFile = "modular.imp";
		perform(conf);
	}

	@Test
	public void testContextSensitiveRTA() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.outputs.add(new JSONCallGraph<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new Sign(),
				DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "context";
		conf.programFile = "context.imp";
		perform(conf);
	}

	@Test
	public void testContextSensitiveRTAHelper() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.outputs.add(new JSONCallGraph<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new Sign(),
				DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "context-helper-last";
		conf.programFile = "context-helper.imp";
		perform(conf);
	}

	@Test
	public void testContextSensitiveRTAHelperFullStack() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.outputs.add(new JSONCallGraph<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new Sign(),
				DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(-1);
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "context-helper-full";
		conf.programFile = "context-helper.imp";
		perform(conf);
	}

	@Test
	public void testContextSensitiveRTAArrayOpPP() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.outputs.add(new JSONCallGraph<>());
		conf.analysis = DefaultConfiguration
				.simpleDomain(new PointBasedHeap(), new Interval(), DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "context-pp-arrayop";
		conf.programFile = "array-op.imp";
		perform(conf);
	}

	@Test
	public void testContextSensitiveRTATwoArraysPP() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.outputs.add(new JSONCallGraph<>());
		conf.analysis = DefaultConfiguration
				.simpleDomain(new PointBasedHeap(), new Interval(), DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "context-pp-twoarrays";
		conf.programFile = "two-arrays.imp";
		perform(conf);
	}

	@Test
	public void issue324()
			throws ParsingException,
			AnalysisException {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.outputs.add(new JSONCallGraph<>());
		conf.analysis = DefaultConfiguration
				.simpleDomain(new FieldSensitivePointBasedHeap(), new IntegerConstantPropagation(),
						new InferredTypes());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "issues";
		conf.testSubDir = "324";
		conf.programFile = "324.imp";
		perform(conf);
	}

	@Test
	public void testInliningRTA() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.outputs.add(new JSONCallGraph<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new Sign(),
				DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new InliningAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "inlining";
		conf.programFile = "context.imp";
		perform(conf);
	}

	@Test
	public void testInliningRTAHelper() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.outputs.add(new JSONCallGraph<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new Sign(),
				DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new InliningAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "inlining-helper";
		conf.programFile = "context-helper.imp";
		perform(conf);
	}

	@Test
	public void testInliningRTAArrayOpPP() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.outputs.add(new JSONCallGraph<>());
		conf.analysis = DefaultConfiguration
				.simpleDomain(new PointBasedHeap(), new Interval(), DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new InliningAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "inlining-pp-arrayop";
		conf.programFile = "array-op.imp";
		perform(conf);
	}

	@Test
	public void testInliningRTATwoArraysPP() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.outputs.add(new JSONCallGraph<>());
		conf.analysis = DefaultConfiguration
				.simpleDomain(new PointBasedHeap(), new Interval(), DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new InliningAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "inlining-pp-twoarrays";
		conf.programFile = "two-arrays.imp";
		perform(conf);
	}
}
