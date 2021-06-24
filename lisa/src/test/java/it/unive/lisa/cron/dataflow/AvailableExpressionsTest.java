package it.unive.lisa.cron.dataflow;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import org.junit.Test;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.impl.dataflow.AvailableExpressions;

public class AvailableExpressionsTest extends AnalysisTestExecutor {

	@Test
	public void testAvailableExpressions() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration().setDumpAnalysis(true).setAbstractState(
				getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new AvailableExpressions()));
		perform("available-expressions", "available-expressions.imp", conf);
	}
}