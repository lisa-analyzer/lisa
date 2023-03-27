package it.unive.lisa.cron.typeInference;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.analysis.value.ValueDomain;
import org.junit.Test;

public class TypesCollectionTest extends AnalysisTestExecutor {
	@Test
	public void testTypesCollection() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				getDefaultFor(ValueDomain.class),
				new TypeEnvironment<>(new InferredTypes()));
		conf.testDir = "type-inference";
		conf.programFile = "program.imp";
		perform(conf);
	}
}
