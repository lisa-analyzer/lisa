package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.util.numeric.MathNumberConversionException;

public class RepeatTest {

	@Test
    public void repeatTest001() throws MathNumberConversionException{
        SimpleAutomaton a = new SimpleAutomaton("a");
        assertEquals(a.repeat(new Interval(2, 2)), new SimpleAutomaton("aa"));
    }
	
	@Test
    public void repeatTest002() throws MathNumberConversionException{
		SimpleAutomaton a = new SimpleAutomaton("alpha");
        assertEquals(a.repeat(new Interval(3, 3)), new SimpleAutomaton("alphaalphaalpha"));
    }
	
	@Test
    public void repeatTest003() throws MathNumberConversionException{
		SimpleAutomaton a = new SimpleAutomaton("ab").union(new SimpleAutomaton("cd"));
        assertEquals(a.repeat(new Interval(3, 3)), new SimpleAutomaton("ababab").union(new SimpleAutomaton("cdcdcd")));
    }
	
	@Test
    public void repeatTest004() throws MathNumberConversionException{
		SimpleAutomaton a = new SimpleAutomaton("ab").union(new SimpleAutomaton("cd"));
        assertEquals(a.repeat(new Interval(0, 0)), a.emptyString());
    }
}
