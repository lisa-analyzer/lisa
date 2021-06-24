package it.unive.lisa.cron.numeric;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import org.junit.Test;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.impl.numeric.IntegerConstantPropagation;
import it.unive.lisa.analysis.impl.numeric.Interval;
import it.unive.lisa.analysis.impl.numeric.Parity;
import it.unive.lisa.analysis.impl.numeric.Sign;

public class NumericAnalysesTest extends AnalysisTestExecutor {

	@Test
	public void testSign() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration().setDumpAnalysis(true)
				.setAbstractState(getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Sign()));
		perform("sign", "program.imp", conf);
	}

	@Test
	public void testParity() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration().setDumpAnalysis(true)
				.setAbstractState(getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Parity()));
		perform("parity", "program.imp", conf);
	}

	@Test
	public void testInterval() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration().setDumpAnalysis(true)
				.setAbstractState(getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Interval()));
		perform("interval", "program.imp", conf);
	}

	@Test
	public void testIntegerConstantPropagation() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration().setDumpAnalysis(true)
				.setAbstractState(getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class),
						new IntegerConstantPropagation()));
		perform("int-const", "program.imp", conf);
	}
}
