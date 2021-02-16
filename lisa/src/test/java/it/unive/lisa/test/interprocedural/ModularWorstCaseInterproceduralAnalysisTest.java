package it.unive.lisa.test.interprocedural;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.impl.numeric.Sign;
import it.unive.lisa.interprocedural.callgraph.impl.CHACallGraph;
import it.unive.lisa.interprocedural.callgraph.impl.RTACallGraph;
import it.unive.lisa.interprocedural.impl.ModularWorstCaseAnalysis;
import it.unive.lisa.outputs.JsonReport;
import it.unive.lisa.outputs.compare.JsonReportComparer;
import it.unive.lisa.program.Program;
import it.unive.lisa.test.imp.IMPFrontend;
import it.unive.lisa.test.imp.ParsingException;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static it.unive.lisa.LiSAFactory.getDefaultFor;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ModularWorstCaseInterproceduralAnalysisTest {

    private final String filePath = "imp-testcases/interprocedural/program.imp";


    @Test
    public void testCHACallGraph() throws IOException, ParsingException, AnalysisSetupException {
        System.out.println("Testing modular worst case interprocedural analysis with CHA call graph...");
        LiSA lisa = new LiSA();

        Program program = IMPFrontend.processFile(filePath);
        lisa.setProgram(program);
        lisa.setAbstractState(getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Sign()));
        lisa.setDumpAnalysis(true);
        lisa.setJsonOutput(true);
        lisa.setInterproceduralAnalysis(new ModularWorstCaseAnalysis());
        lisa.setCallGraph(new CHACallGraph());
        lisa.setWorkdir("test-outputs/interprocedural/CHA");

        try {
            lisa.run();
        } catch (AnalysisException e) {
            e.printStackTrace(System.err);
            fail("Analysis terminated with errors");
        }

        File actFile = new File("test-outputs/interprocedural/CHA/report.json");
        File expFile = new File("imp-testcases/interprocedural/CHA/report.json");
        JsonReport expected = JsonReport.read(new FileReader(expFile));
        JsonReport actual = JsonReport.read(new FileReader(actFile));

        assertTrue("Results are different",
                JsonReportComparer.compare(expected, actual, expFile.getParentFile(), actFile.getParentFile()));
    }


    @Test
    public void testRTACallGraph() throws IOException, ParsingException, AnalysisSetupException {
        System.out.println("Testing modular worst case interprocedural analysis with RTA call graph...");
        LiSA lisa = new LiSA();

        Program program = IMPFrontend.processFile(filePath);
        lisa.setProgram(program);
        lisa.setAbstractState(getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Sign()));
        lisa.setDumpAnalysis(true);
        lisa.setJsonOutput(true);
        lisa.setInterproceduralAnalysis(new ModularWorstCaseAnalysis());
        lisa.setCallGraph(new RTACallGraph());
        lisa.setWorkdir("test-outputs/interprocedural/RTA");

        try {
            lisa.run();
        } catch (AnalysisException e) {
            e.printStackTrace(System.err);
            fail("Analysis terminated with errors");
        }

        File actFile = new File("test-outputs/interprocedural/RTA/report.json");
        File expFile = new File("imp-testcases/interprocedural/RTA/report.json");
        JsonReport expected = JsonReport.read(new FileReader(expFile));
        JsonReport actual = JsonReport.read(new FileReader(actFile));

        assertTrue("Results are different",
                JsonReportComparer.compare(expected, actual, expFile.getParentFile(), actFile.getParentFile()));
    }
}
