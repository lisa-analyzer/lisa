package it.unive.lisa.test.numeric;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.impl.numeric.IntegerConstantPropagation;
import it.unive.lisa.analysis.impl.numeric.Interval;
import it.unive.lisa.analysis.impl.numeric.Parity;
import it.unive.lisa.analysis.impl.numeric.Sign;
import it.unive.lisa.test.AnalysisTest;
import org.junit.Test;

public class NumericAnalysesTest extends AnalysisTest {

	@Test
	public void testSign() throws AnalysisSetupException {
		perform("sign", "program.imp", false, false,
				getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Sign()));
	}

	@Test
	public void testParity() throws AnalysisSetupException {
		perform("parity", "program.imp", false, false,
				getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Parity()));
	}

	@Test
	public void testInterval() throws AnalysisSetupException {
		perform("interval", "program.imp", false, false,
				getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Interval()));
	}

	@Test
	public void testIntegerConstantPropagation() throws AnalysisSetupException {
		perform("int-const", "program.imp", false, false,
				getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new IntegerConstantPropagation()));
	}
}
