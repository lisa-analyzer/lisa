package it.unive.lisa.test.typeInference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;

import org.junit.Test;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.nonrelational.typeInference.TypeInference;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.test.imp.IMPFrontend;

public class TypeInferenceTest {
	@Test
	public void testSyntacticChecks() throws IOException {
		LiSA lisa = new LiSA();

		Collection<CFG> cfgs = IMPFrontend.processFile("imp-testcases/type-inference/program.imp");
		cfgs.forEach(lisa::addCFG);
		lisa.addNonRelationalValueDomain(new TypeInference());
		try {
			lisa.run();
		} catch (AnalysisException e) {
			System.err.println(e);
			fail("Analysis terminated with errors");
		}
	}
}
