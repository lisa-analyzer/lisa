package it.unive.lisa.cron.dataflow;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.dataflow.AvailableExpressions;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import org.junit.Test;

public class AvailableExpressionsTest extends AnalysisTestExecutor {

	@Test
	public void testAvailableExpressions() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new AvailableExpressions(),
				getDefaultFor(TypeDomain.class));
		perform("available-expressions", "available-expressions.imp", conf);
	}
}