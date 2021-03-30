package it.unive.lisa.cron.interprocedural;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import org.junit.Test;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.impl.numeric.Sign;
import it.unive.lisa.interprocedural.callgraph.impl.CHACallGraph;
import it.unive.lisa.interprocedural.callgraph.impl.RTACallGraph;
import it.unive.lisa.interprocedural.impl.ModularWorstCaseAnalysis;

@SuppressWarnings("rawtypes")
public class ModularWorstCaseInterproceduralAnalysisTest extends AnalysisTestExecutor {

	@Test
	public void testCHACallGraph() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration().setInferTypes(true)
				.setAbstractState(getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Sign()))
				.setDumpAnalysis(true)
				.setInterproceduralAnalysis(new ModularWorstCaseAnalysis())
				.setCallGraph(new CHACallGraph());
		perform("interprocedural", "CHA", "program.imp", conf);
	}

	@Test
	public void testRTACallGraph() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration().setInferTypes(true)
				.setAbstractState(getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Sign()))
				.setDumpAnalysis(true)
				.setInterproceduralAnalysis(new ModularWorstCaseAnalysis())
				.setCallGraph(new RTACallGraph());
		perform("interprocedural", "RTA", "program.imp", conf);
	}
}
