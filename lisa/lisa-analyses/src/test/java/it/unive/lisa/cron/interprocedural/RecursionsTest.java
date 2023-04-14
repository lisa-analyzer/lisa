package it.unive.lisa.cron.interprocedural;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.ContextInsensitiveToken;
import it.unive.lisa.interprocedural.context.KDepthToken;
import it.unive.lisa.interprocedural.context.LastCallToken;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RecursionsTest extends AnalysisTestExecutor {

	@Test
	public void testFibonacciKDepth() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(KDepthToken.getSingleton(5));
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "fibonacci/kdepth";
		conf.programFile = "fibonacci.imp";
		// as the test uses addition, there is a spurious x strcat y placed in
		// the computed expressions in the first fixpoint round as the types of
		// x and y cannot be determined (the state is bottom). This does not
		// happen when unwinding the results, causing a difference.
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	public void testFibonacciLast() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(LastCallToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "fibonacci/last";
		conf.programFile = "fibonacci.imp";
		// as the test uses addition, there is a spurious x strcat y placed in
		// the computed expressions in the first fixpoint round as the types of
		// x and y cannot be determined (the state is bottom). This does not
		// happen when unwinding the results, causing a difference.
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	public void testFibonacciInsensitive() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(ContextInsensitiveToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "fibonacci/insensitive";
		conf.programFile = "fibonacci.imp";
		// as the test uses addition, there is a spurious x strcat y placed in
		// the computed expressions in the first fixpoint round as the types of
		// x and y cannot be determined (the state is bottom). This does not
		// happen when unwinding the results, causing a difference.
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	public void testFactorialLoopKDepth() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(KDepthToken.getSingleton(5));
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "factorialLoop/kdepth";
		conf.programFile = "factorialLoop.imp";
		perform(conf);
	}

	@Test
	public void testFactorialLoopLast() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(LastCallToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "factorialLoop/last";
		conf.programFile = "factorialLoop.imp";
		perform(conf);
	}

	@Test
	public void testFactorialLoopInsensitive() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(ContextInsensitiveToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "factorialLoop/insensitive";
		conf.programFile = "factorialLoop.imp";
		perform(conf);
	}

	@Test
	public void testInfiniteRecursion2KDepth() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(KDepthToken.getSingleton(5));
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "infiniteRecursion2/kdepth";
		conf.programFile = "infiniteRecursion2.imp";
		// note: the result of this recursion is bottom as it never terminates
		perform(conf);
	}

	@Test
	public void testInfiniteRecursion2Last() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(LastCallToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "infiniteRecursion2/last";
		conf.programFile = "infiniteRecursion2.imp";
		// note: the result of this recursion is bottom as it never terminates
		perform(conf);
	}

	@Test
	public void testInfiniteRecursion2Insensitive() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(ContextInsensitiveToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "infiniteRecursion2/insensitive";
		conf.programFile = "infiniteRecursion2.imp";
		// note: the result of this recursion is bottom as it never terminates
		perform(conf);
	}

	@Test
	public void testInfiniteRecursion1KDepth() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(KDepthToken.getSingleton(5));
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "infiniteRecursion1/kdepth";
		conf.programFile = "infiniteRecursion1.imp";
		// note: the result of this recursion is bottom as it never terminates
		perform(conf);
	}

	@Test
	public void testInfiniteRecursion1Last() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(LastCallToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "infiniteRecursion1/last";
		conf.programFile = "infiniteRecursion1.imp";
		// note: the result of this recursion is bottom as it never terminates
		perform(conf);
	}

	@Test
	public void testInfiniteRecursion1Insensitive() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(ContextInsensitiveToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "infiniteRecursion1/insensitive";
		conf.programFile = "infiniteRecursion1.imp";
		// note: the result of this recursion is bottom as it never terminates
		perform(conf);
	}

	@Test
	public void testFactorialKDepth() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(KDepthToken.getSingleton(5));
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "factorial/kdepth";
		conf.programFile = "factorial.imp";
		perform(conf);
	}

	@Test
	public void testFactorialLast() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(LastCallToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "factorial/last";
		conf.programFile = "factorial.imp";
		perform(conf);
	}

	@Test
	public void testFactorialInsensitive() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(ContextInsensitiveToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "factorial/insensitive";
		conf.programFile = "factorial.imp";
		perform(conf);
	}

	@Test
	public void testFactorialInterleavedKDepth() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(KDepthToken.getSingleton(5));
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "factorialInterleaved/kdepth";
		conf.programFile = "factorialInterleaved.imp";
		perform(conf);
	}

	@Test
	public void testFactorialInterleavedLast() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(LastCallToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "factorialInterleaved/last";
		conf.programFile = "factorialInterleaved.imp";
		perform(conf);
	}

	@Test
	public void testFactorialInterleavedInsensitive() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(ContextInsensitiveToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "factorialInterleaved/insensitive";
		conf.programFile = "factorialInterleaved.imp";
		perform(conf);
	}

	@Test
	public void testTwoRecursionsKDepth() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(KDepthToken.getSingleton(5));
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "twoRecursions/kdepth";
		conf.programFile = "twoRecursions.imp";
		perform(conf);
	}

	@Test
	public void testTwoRecursionsLast() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(LastCallToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "twoRecursions/last";
		conf.programFile = "twoRecursions.imp";
		perform(conf);
	}

	@Test
	public void testTwoRecursionsInsensitive() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(ContextInsensitiveToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "twoRecursions/insensitive";
		conf.programFile = "twoRecursions.imp";
		perform(conf);
	}
}
