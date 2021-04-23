package it.unive.lisa.cron.interprocedural;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.impl.numeric.Sign;
import it.unive.lisa.interprocedural.callgraph.impl.RTACallGraph;
import it.unive.lisa.interprocedural.impl.CallPointContextSensitiveToken;
import it.unive.lisa.interprocedural.impl.ContextSensitiveAnalysis;
import org.junit.Test;

@SuppressWarnings("rawtypes")
public class ContextSensitiveInterproceduralAnalysisTest extends AnalysisTestExecutor {

	@Test
	public void testRTAContextSensitive1() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration().setInferTypes(true)
				.setAbstractState(getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Sign()))
				.setDumpAnalysis(true)
				.setInterproceduralAnalysis(
						new ContextSensitiveAnalysis(CallPointContextSensitiveToken.getSingleton()))
				.setCallGraph(new RTACallGraph());
		perform("interprocedural", "RTAContextSensitive1", "programContextSensitive1.imp", conf);
	}

	@Test
	public void testRTAContextSensitive2() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration().setInferTypes(true)
				.setAbstractState(getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Sign()))
				.setDumpAnalysis(true)
				.setInterproceduralAnalysis(
						new ContextSensitiveAnalysis(CallPointContextSensitiveToken.getSingleton()))
				.setCallGraph(new RTACallGraph());
		perform("interprocedural", "RTAContextSensitive2", "programContextSensitive2.imp", conf);
	}

	@Test
	public void testRTAContextSensitive3() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration().setInferTypes(true)
				.setAbstractState(getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Sign()))
				.setDumpAnalysis(true)
				.setInterproceduralAnalysis(
						new ContextSensitiveAnalysis(CallPointContextSensitiveToken.getSingleton()))
				.setCallGraph(new RTACallGraph());
		perform("interprocedural", "RTAContextSensitive3", "programContextSensitive3.imp", conf);
	}

}
