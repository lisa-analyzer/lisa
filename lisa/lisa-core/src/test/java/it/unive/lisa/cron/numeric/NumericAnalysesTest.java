package it.unive.lisa.cron.numeric;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.numeric.IntegerConstantPropagation;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.numeric.Parity;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.types.InferredTypes;
import org.junit.Test;

public class NumericAnalysesTest extends AnalysisTestExecutor {

	@Test
	public void testSign() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Sign(),
				new TypeEnvironment<>(new InferredTypes()));
		perform("sign", "program.imp", conf);
	}

	@Test
	public void testParity() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Parity(),
				new TypeEnvironment<>(new InferredTypes()));
		perform("parity", "program.imp", conf);
	}

	@Test
	public void testInterval() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Interval(),
				new TypeEnvironment<>(new InferredTypes()));
		perform("interval", "program.imp", conf);
	}

	@Test
	public void testIntegerConstantPropagation() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class),
				new IntegerConstantPropagation(),
				new TypeEnvironment<>(new InferredTypes()));
		perform("int-const", "program.imp", conf);
	}
}
