package it.unive.lisa.cron.string;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import org.junit.Test;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.string.CharInclusion;
import it.unive.lisa.analysis.types.InferredTypes;

public class CharInclusionAnalysisTest extends AnalysisTestExecutor {

	@Test
	public void testCharInclusion() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new CharInclusion(),
				new TypeEnvironment<>(new InferredTypes()));
		conf.testDir = "char-inclusion";
		conf.programFile = "program.imp";
		perform(conf);
	}
}