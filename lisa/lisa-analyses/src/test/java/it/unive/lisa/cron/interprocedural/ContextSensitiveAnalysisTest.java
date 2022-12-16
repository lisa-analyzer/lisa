package it.unive.lisa.cron.interprocedural;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import org.junit.Test;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.LiSAConfiguration.GraphType;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.interprocedural.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.ContextInsensitiveToken;
import it.unive.lisa.interprocedural.KDepthToken;
import it.unive.lisa.interprocedural.LastScopeToken;
import it.unive.lisa.interprocedural.RecursionFreeToken;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;

public class ContextSensitiveAnalysisTest extends AnalysisTestExecutor {

	@Test
	public void testRTAContextSensitive1() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Sign(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "RTAContextSensitive1";
		conf.programFile = "programContextSensitive1.imp";
		perform(conf);
	}

	@Test
	public void testRTAContextSensitive2() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Sign(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "RTAContextSensitive2";
		conf.programFile = "programContextSensitive2.imp";
		perform(conf);
	}

	@Test
	public void testRTAContextSensitive3() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Sign(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "RTAContextSensitive3";
		conf.programFile = "programContextSensitive3.imp";
		perform(conf);
	}

	@Test
	public void testRTAContextSensitive4() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Sign(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(RecursionFreeToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "RTAContextSensitive4";
		conf.programFile = "programContextSensitive3.imp";
		perform(conf);
	}

	@Test
	public void testRTAContextSensitive5() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				new PointBasedHeap(),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "RTAContextSensitive5";
		conf.programFile = "programContextSensitive4.imp";
		perform(conf);
	}

	@Test
	public void testRTAContextSensitive6() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				new PointBasedHeap(),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "RTAContextSensitive6";
		conf.programFile = "programContextSensitive5.imp";
		perform(conf);
	}

	@Test
	public void testRecursionWithRecFreeToken() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.analysisGraphs = GraphType.HTML_WITH_SUBNODES;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(RecursionFreeToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		perform("interprocedural", "recRecFreeToken", "recursion.imp", conf);
	}

	@Test
	public void testRecursionWithLastToken() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.analysisGraphs = GraphType.HTML_WITH_SUBNODES;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(LastScopeToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		perform("interprocedural", "recLastToken", "recursion.imp", conf);
	}

	@Test
	public void testRecursionWithKDepthToken() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.analysisGraphs = GraphType.HTML_WITH_SUBNODES;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(KDepthToken.getSingleton(5));
		conf.callGraph = new RTACallGraph();
		perform("interprocedural", "recKDepthToken", "recursion.imp", conf);
	}

	@Test
	public void testRecursionWithInsensitiveToken() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.analysisGraphs = GraphType.HTML_WITH_SUBNODES;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(ContextInsensitiveToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		perform("interprocedural", "recInsensitiveToken", "recursion.imp", conf);
	}
}
