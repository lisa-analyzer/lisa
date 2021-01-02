package it.unive.lisa.test.imp.test;

import static org.junit.Assert.fail;

import it.unive.lisa.test.imp.IMPFrontend;
import it.unive.lisa.test.imp.ParsingException;
import org.junit.Test;

public class IMPFrontendTest {

	@Test
	public void testExampleProgram() {
		try {
			IMPFrontend.processFile("imp-testcases/example.imp");
		} catch (ParsingException e) {
			fail("Processing the example file thrown an exception: " + e);
		}
	}
}
