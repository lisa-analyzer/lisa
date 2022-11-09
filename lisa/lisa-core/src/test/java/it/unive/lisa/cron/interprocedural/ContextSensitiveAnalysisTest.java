package it.unive.lisa.cron.interprocedural;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.interprocedural.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.RecursionFreeToken;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import org.junit.Test;

public class ContextSensitiveAnalysisTest extends AnalysisTestExecutor {

	@Test
	public void testRTAContextSensitive1() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Sign(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		perform("interprocedural", "RTAContextSensitive1", "programContextSensitive1.imp", conf);
	}

	@Test
	public void testRTAContextSensitive2() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Sign(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		perform("interprocedural", "RTAContextSensitive2", "programContextSensitive2.imp", conf);
	}

	@Test
	public void testRTAContextSensitive3() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Sign(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		perform("interprocedural", "RTAContextSensitive3", "programContextSensitive3.imp", conf);
	}

	@Test
	public void testRTAContextSensitive4() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Sign(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(RecursionFreeToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		perform("interprocedural", "RTAContextSensitive4", "programContextSensitive3.imp", conf);
	}

	@Test
	public void testRTAContextSensitive5() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				new PointBasedHeap(),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		perform("interprocedural", "RTAContextSensitive5", "programContextSensitive4.imp", conf);
	}

	@Test
	public void testRTAContextSensitive6() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				new PointBasedHeap(),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		perform("interprocedural", "RTAContextSensitive6", "programContextSensitive5.imp", conf);
	}

}
