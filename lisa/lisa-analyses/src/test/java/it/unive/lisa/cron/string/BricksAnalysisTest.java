package it.unive.lisa.cron.string;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.string.Bricks;
import it.unive.lisa.analysis.types.InferredTypes;
import org.junit.Test;

public class BricksAnalysisTest extends AnalysisTestExecutor {

	@Test
	public void testBricks() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Bricks(),
				new TypeEnvironment<>(new InferredTypes()));
		conf.analysisGraphs = LiSAConfiguration.GraphType.HTML;

		perform("bricks", "program.imp", conf);
	}
}