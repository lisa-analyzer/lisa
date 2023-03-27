package it.unive.lisa.cron.dataflow;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.dataflow.AvailableExpressions;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import org.junit.Test;

public class AvailableExpressionsTest extends AnalysisTestExecutor {

	@Test
	public void testAvailableExpressions() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new AvailableExpressions(),
				getDefaultFor(TypeDomain.class));
		conf.testDir = "available-expressions";
		conf.programFile = "available-expressions.imp";
		perform(conf);
	}
}