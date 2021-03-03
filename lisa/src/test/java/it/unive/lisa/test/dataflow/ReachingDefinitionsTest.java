package it.unive.lisa.test.dataflow;

import org.junit.Test;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.LiSA;
import it.unive.lisa.LiSAFactory;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.dataflow.PossibleForwardDataflowDomain;
import it.unive.lisa.analysis.dataflow.impl.ReachingDefinitions;
import it.unive.lisa.program.Program;
import it.unive.lisa.test.imp.IMPFrontend;
import it.unive.lisa.test.imp.ParsingException;

public class ReachingDefinitionsTest {

	// we mark the method as a junit test, so that gradle knows
	// that it must be executed as part of the build
	@Test
	public void testReachingDefinitions() throws ParsingException, AnalysisException {
		// first step: create a lisa instance
		LiSA lisa = new LiSA();

		// second step: tell lisa which analysis has to be executed
		// this is done by composing the abstract state for the analysis, selecting:
		// - the kind of abstract state that we want
		// - the kind of heap domain that we want
		// - the kind of value domain that we want

		// the value domain is our reaching definition analysis, that
		// has to be wrapped in a possible dataflow domain
		ValueDomain<?> valueDomain = new PossibleForwardDataflowDomain<>(new ReachingDefinitions());

		// we do not care about which heap domain is executed, so we can go with
		// whatever lisa considers as default implementation
		HeapDomain<?> heapDomain = LiSAFactory.getDefaultFor(HeapDomain.class);

		// we do not care either about the kind of abstract state that is executed,
		// so we can use the default implementation. We must pass the heap domain
		// and the value domain as parameters, so that exactly the one that we chose
		// will be used at runtime
		AbstractState<?, ?, ?> abstractState = LiSAFactory.getDefaultFor(AbstractState.class, heapDomain, valueDomain);

		// we can finally tell lisa which is the abstract state for the analysis
		lisa.setAbstractState(abstractState);
		
		// third step: we tell lisa that we want our .dot files to be dumped,  
		// so that we can inspect the results of the analysis
		lisa.setDumpAnalysis(true);
		
		// fourth step: we tell lisa which directory should be used as working 
		// directory, where all .dot files will be dumped
		lisa.setWorkdir("test-outputs/reaching-definitions");
		
		// fifth step: we tell lisa what to analyze 
		// we first parse the imp file using the imp frontend, and then we can feed
		// the result of the parsing to lisa
		Program impProgram = IMPFrontend.processFile("reaching-definitions.imp");
		lisa.setProgram(impProgram);
		
		// sixth step: run the analysis!
		lisa.run();
	}
}
