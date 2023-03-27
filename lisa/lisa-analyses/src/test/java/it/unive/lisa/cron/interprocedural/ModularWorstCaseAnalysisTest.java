package it.unive.lisa.cron.interprocedural;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.interprocedural.ModularWorstCaseAnalysis;
import it.unive.lisa.interprocedural.callgraph.CHACallGraph;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import org.junit.Test;

public class ModularWorstCaseAnalysisTest extends AnalysisTestExecutor {

	@Test
	public void testCHACallGraph() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Sign(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ModularWorstCaseAnalysis<>();
		conf.callGraph = new CHACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "CHA";
		conf.programFile = "program.imp";
		perform(conf);
	}

	@Test
	public void testRTACallGraph() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Sign(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ModularWorstCaseAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "RTA";
		conf.programFile = "program.imp";
		perform(conf);
	}
}
