package it.unive.lisa.cron.dataflow;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.dataflow.ConstantPropagation;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import org.junit.Test;

public class ConstantPropagationDFTest extends AnalysisTestExecutor {

	@Test
	public void testConstantPropagation() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new ConstantPropagation(),
				getDefaultFor(TypeDomain.class));
		conf.testDir = "constant-propagation-df";
		conf.programFile = "constant-propagation.imp";
		perform(conf);
	}
}
