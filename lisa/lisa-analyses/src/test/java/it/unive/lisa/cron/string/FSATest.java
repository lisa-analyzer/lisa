package it.unive.lisa.cron.string;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import org.junit.Test;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.string.fsa.FSA;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.conf.LiSAConfiguration;

public class FSATest extends AnalysisTestExecutor {

	@Test
	public void testFSA() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new FSA(),
				new TypeEnvironment<>(new InferredTypes()));
		perform("fsa", "program.imp", conf);
	}
}
