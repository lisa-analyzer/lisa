package it.unive.lisa.test.typeInference;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.LiSA;
import it.unive.lisa.outputs.JsonReport;
import it.unive.lisa.outputs.compare.JsonReportComparer;
import it.unive.lisa.program.Program;
import it.unive.lisa.test.imp.IMPFrontend;
import it.unive.lisa.test.imp.ParsingException;

public class TypesCollectionTest {
	@Test
	public void testTypesCollection() throws IOException, ParsingException {
		System.out.println("Testing types collection...");
		LiSA lisa = new LiSA();

		Program program = IMPFrontend.processFile("imp-testcases/type-inference/program.imp");
		lisa.setProgram(program);
		lisa.setInferTypes(true);
		lisa.setDumpTypeInference(true);
		lisa.setJsonOutput(true);
		lisa.setWorkdir("test-outputs/type-inference");
		try {
			lisa.run();
		} catch (AnalysisException e) {
			e.printStackTrace(System.err);
			fail("Analysis terminated with errors");
		}

		File expFile = new File("imp-testcases/type-inference/report.json");
		File actFile = new File("test-outputs/type-inference/report.json");
		JsonReport expected = JsonReport.read(new FileReader(expFile));
		JsonReport actual = JsonReport.read(new FileReader(actFile));

		assertTrue("Results are different",
				JsonReportComparer.compare(expected, actual, expFile.getParentFile(), actFile.getParentFile()));
	}
}
