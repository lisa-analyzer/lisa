package it.unive.lisa.imp;

import static org.junit.Assert.fail;

import org.junit.Test;

import it.unive.lisa.LiSA;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.program.Program;

public class IMPFrontendTest {

	@Test
	public void testExampleProgram() {
		try {
			IMPFrontend.processFile("example.imp", false);
		} catch (ParsingException e) {
			fail("Processing the example file thrown an exception: " + e);
		}
	}

	@Test
	public void testErrors() {
		try {
			Program program = IMPFrontend.processFile("imp-testcases/try-catch.imp", false);
			LiSAConfiguration conf = new LiSAConfiguration();
			conf.workdir = "target/test-home/errors";
			conf.jsonOutput = true;
			conf.serializeInputs = true;
			conf.analysisGraphs = LiSAConfiguration.GraphType.HTML_WITH_SUBNODES;
			LiSA l = new LiSA(conf);
			l.run(program);
		} catch (ParsingException e) {
			fail("Processing the try-catch file thrown an exception: " + e);
		}
	}

}
