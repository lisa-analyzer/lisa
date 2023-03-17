package it.unive.lisa.cron.string;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.string.bricks.Bricks;
import it.unive.lisa.analysis.types.InferredTypes;
import org.junit.Test;

public class BricksAnalysisTest extends AnalysisTestExecutor {

	@Test
	public void testBricks() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Bricks(),
				new TypeEnvironment<>(new InferredTypes()));
		conf.testDir = "bricks";
		conf.programFile = "program.imp";
		// we disable optimized test because of bricks normalization: without
		// optimization, loops that get iterated more than once will have
		// poststates of instructions within them built with at least one lub
		// invocation between the different iterations, and that will invoke the
		// normalization algorithm. Optimized run instead will not iterate
		// multiple times, and poststates will be the plain ones returned by
		// abstract transformers. Even if they are semantically equivalent,
		// comparisons will fail nonetheless
		conf.compareWithOptimization = false;
		perform(conf);
	}
}