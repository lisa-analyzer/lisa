package it.unive.lisa.cron.interprocedural;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import org.junit.Test;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.interprocedural.ModularWorstCaseAnalysis;
import it.unive.lisa.interprocedural.callgraph.CHACallGraph;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;

@SuppressWarnings("rawtypes")
public class ModularWorstCaseAnalysisTest extends AnalysisTestExecutor {

	@Test
	public void testCHACallGraph() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration()
				.setAbstractState(getDefaultFor(AbstractState.class,
						getDefaultFor(HeapDomain.class),
						new Sign(),
						getDefaultFor(TypeDomain.class)))
				.setSerializeResults(true)
				.setInterproceduralAnalysis(new ModularWorstCaseAnalysis())
				.setCallGraph(new CHACallGraph());
		perform("interprocedural", "CHA", "program.imp", conf);
	}

	@Test
	public void testRTACallGraph() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration()
				.setAbstractState(getDefaultFor(AbstractState.class,
						getDefaultFor(HeapDomain.class),
						new Sign(),
						getDefaultFor(TypeDomain.class)))
				.setSerializeResults(true)
				.setInterproceduralAnalysis(new ModularWorstCaseAnalysis())
				.setCallGraph(new RTACallGraph());
		perform("interprocedural", "RTA", "program.imp", conf);
	}
}
