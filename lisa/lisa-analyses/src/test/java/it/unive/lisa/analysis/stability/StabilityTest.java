package it.unive.lisa.analysis.stability;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.Stability;
import it.unive.lisa.analysis.Trend;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;
import org.junit.Test;

import static org.junit.Assert.*;

public class StabilityTest {

    Interval interval1 = new Interval(3, 5);
    Interval interval2 = new Interval(3, 4);

    Trend trendTOP = new Trend((byte) 0); // TOP
    Trend trendBOTTOM = new Trend((byte) 1); // BOTTOM

    Trend trendSTABLE = new Trend((byte) 2); // STABLE
    Trend trendINC = new Trend((byte) 3); // INC
    Trend trendDEC = new Trend((byte) 4); // DEC
    Trend trendNON_DEC = new Trend((byte) 5); // NON_DEC
    Trend trendNON_INC = new Trend((byte) 6); // NON_INC
    Trend trendNON_STABLE = new Trend((byte) 7); // NON_STABLE

    Stability s1top = new Stability(
            new ValueEnvironment<>(interval1),
            new ValueEnvironment<>(trendTOP)
    );

    Stability s1bot = new Stability(
            new ValueEnvironment<>(interval1),
            new ValueEnvironment<>(trendBOTTOM)
    );

    Stability s1st = new Stability(
            new ValueEnvironment<>(interval1),
            new ValueEnvironment<>(trendSTABLE)
    );

    Stability s1in = new Stability(
            new ValueEnvironment<>(interval1),
            new ValueEnvironment<>(trendINC)
    );


    Stability s1de = new Stability(
            new ValueEnvironment<>(interval1),
            new ValueEnvironment<>(trendDEC)
    );


    Stability s1nd = new Stability(
            new ValueEnvironment<>(interval1),
            new ValueEnvironment<>(trendNON_DEC)
    );


    Stability s1ni = new Stability(
            new ValueEnvironment<>(interval1),
            new ValueEnvironment<>(trendNON_INC)
    );


    Stability s1ns = new Stability(
            new ValueEnvironment<>(interval1),
            new ValueEnvironment<>(trendNON_STABLE)
    );

    Stability s2top = new Stability(
            new ValueEnvironment<>(interval2),
            new ValueEnvironment<>(trendTOP)
    );

    Stability s2bot = new Stability(
            new ValueEnvironment<>(interval2),
            new ValueEnvironment<>(trendBOTTOM)
    );

    Stability s2st = new Stability(
            new ValueEnvironment<>(interval2),
            new ValueEnvironment<>(trendSTABLE)
    );

    Stability s2in = new Stability(
            new ValueEnvironment<>(interval2),
            new ValueEnvironment<>(trendINC)
    );


    Stability s2de = new Stability(
            new ValueEnvironment<>(interval2),
            new ValueEnvironment<>(trendDEC)
    );


    Stability s2nd = new Stability(
            new ValueEnvironment<>(interval2),
            new ValueEnvironment<>(trendNON_DEC)
    );


    Stability s2ni = new Stability(
            new ValueEnvironment<>(interval2),
            new ValueEnvironment<>(trendNON_INC)
    );

    Stability s2ns = new Stability(
            new ValueEnvironment<>(interval2),
            new ValueEnvironment<>(trendNON_STABLE)
    );


    @Test
    public void testExample() throws ParsingException, AnalysisException {

        // we parse the program to get the CFG representation of the code in it
        Program program = IMPFrontend.processFile("imp-testcases/numeric/numeric.imp");

        // we build a new configuration for the analysis
        LiSAConfiguration conf = new DefaultConfiguration();

        // we specify where we want files to be generated
        conf.workdir = "output";

        // we specify the visual format of the analysis results
        conf.analysisGraphs = LiSAConfiguration.GraphType.HTML;

        // we specify the analysis that we want to execute
        conf.abstractState = new SimpleAbstractState<>(
                // heap domain
                new MonolithicHeap(),
                // value domain
                new Stability(),
                // type domain
                new TypeEnvironment<>(new InferredTypes()));

        // we instantiate LiSA with our configuration
        LiSA lisa = new LiSA(conf);

        // finally, we tell LiSA to analyze the program
        lisa.run(program);
    }

    @Test
    public void test_Equals() {

        assertEquals(s1ns, s1ns);

        Stability s1ns_bis = new Stability(
                new ValueEnvironment<>(interval1),
                new ValueEnvironment<>(trendNON_STABLE)
        );

        assertEquals(s1ns, s1ns_bis);

        assertNotEquals(s1ns, s2ns);
        assertNotEquals(s1ns, s1in);
        assertNotEquals(s2ns, s1in);
    }

    @Test
    public void test_LessOrEqual() throws SemanticException {

        /*
        assertTrue(trendNON_STABLE.lessOrEqualAux(trendTOP));
        assertTrue(trendBOTTOM.lessOrEqualAux(trendNON_STABLE));
        assertFalse(trendTOP.lessOrEqualAux(trendNON_STABLE));
        assertFalse(trendNON_STABLE.lessOrEqualAux(trendBOTTOM));
         */

        assertTrue(s1ns.lessOrEqual(s1ns));

        // interval <= interval && trend <= trend
        assertTrue(s2in.lessOrEqual(s1ns)); // {[3,4], INC} Le {[3,5], NON_STABLE}
        assertTrue(s2in.lessOrEqual(s1in)); // {[3,4], INC} <= {[3,5], INC}
        assertTrue(s2in.lessOrEqual(s2ns)); // {[3,4], INC} <= {[3,4], NON_STABLE}

        // interval <= interval && trend !<= trend
        assertFalse(s2ns.lessOrEqual(s1in)); // {[3,4], NON_STABLE} !<= {[3,5], INC}

        // interval !<= interval && trend <= trend
        assertFalse(interval1.lessOrEqual(interval2));
        assertFalse(s1in.lessOrEqual(s2in)); // {[3,5], INC} !<= {[3,4], INC}

        // interval !<= interval && trend !<=trend
        assertFalse(s1ns.lessOrEqual(s2in)); // {[3,5], NON_STABLE} !<= {[3,4], INC}

        // interval <= interval && TOP !<= trend
        assertFalse(s2top.lessOrEqual(s1ns));
        assertTrue(s2ns.lessOrEqual(s2top));

        // interval !<= interval && BOTTOM <= trend
        assertFalse(s1bot.lessOrEqual(s2in));
    }

    @Test
    public void test_Lub() throws SemanticException {

        assertEquals(s1in, s1in.lub(s1in));

        // lub({[3,5], INC}, {[3,5], DEC}) = {[3,5], NON_STABLE}
        assertEquals(s1ns, s1in.lub(s1de));
        assertEquals(s1ns, s1de.lub(s1in));

        // lub({[3,5], INC}, {[3,5], NON_STABLE}) = {[3,5], NON_STABLE}
        assertEquals(s1ns, s1in.lub(s1ns));
        assertEquals(s1ns, s1ns.lub(s1in));

        // lub({[3,5], NON_DEC}, {[3,5], DEC}) = {[3,5], TOP}
        assertEquals(s1top, s1nd.lub(s1de));
    }
}
