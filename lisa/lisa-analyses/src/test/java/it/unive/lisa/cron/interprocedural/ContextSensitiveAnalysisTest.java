package it.unive.lisa.cron.interprocedural;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.interprocedural.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.ContextInsensitiveToken;
import it.unive.lisa.interprocedural.KDepthToken;
import it.unive.lisa.interprocedural.LastCallToken;
import it.unive.lisa.interprocedural.RecursionFreeToken;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import org.junit.Ignore;
import org.junit.Test;

public class ContextSensitiveAnalysisTest extends AnalysisTestExecutor {

	@Test
	public void testRTAContextSensitive1() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Sign(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "RTAContextSensitive1";
		conf.programFile = "programContextSensitive1.imp";
		perform(conf);
	}

	@Test
	public void testRTAContextSensitive2() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Sign(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "RTAContextSensitive2";
		conf.programFile = "programContextSensitive2.imp";
		perform(conf);
	}

	@Test
	public void testRTAContextSensitive3() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Sign(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "RTAContextSensitive3";
		conf.programFile = "programContextSensitive3.imp";
		perform(conf);
	}

	@Test
	public void testRTAContextSensitive4() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Sign(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(RecursionFreeToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "RTAContextSensitive4";
		conf.programFile = "programContextSensitive3.imp";
		perform(conf);
	}

	@Test
	public void testRTAContextSensitive5() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				new PointBasedHeap(),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "RTAContextSensitive5";
		conf.programFile = "programContextSensitive4.imp";
		perform(conf);
	}

	@Test
	public void testRTAContextSensitive6() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				new PointBasedHeap(),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "RTAContextSensitive6";
		conf.programFile = "programContextSensitive5.imp";
		perform(conf);
	}

	@Test
	@Ignore
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
		// during the first evaluation of the recursive chain, having a bottom
		// value means that + can also be rewritten as strcat. This does not
		// happen when the full chain is computed (when unwinding the results)
		// and will thus cause a diff in results due to the missing expression
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	@Ignore
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
		// during the first evaluation of the recursive chain, having a bottom
		// value means that + can also be rewritten as strcat. This does not
		// happen when the full chain is computed (when unwinding the results)
		// and will thus cause a diff in results due to the missing expression
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	@Ignore
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
		// during the first evaluation of the recursive chain, having a bottom
		// value means that + can also be rewritten as strcat. This does not
		// happen when the full chain is computed (when unwinding the results)
		// and will thus cause a diff in results due to the missing expression
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	@Ignore
	public void testLoopKDepth() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(KDepthToken.getSingleton(5));
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "loop/kdepth";
		conf.programFile = "loop.imp";
		// during the first evaluation of the recursive chain, having a bottom
		// value means that + can also be rewritten as strcat. This does not
		// happen when the full chain is computed (when unwinding the results)
		// and will thus cause a diff in results due to the missing expression
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	@Ignore
	public void testLoopLast() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(LastCallToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "loop/last";
		conf.programFile = "loop.imp";
		// during the first evaluation of the recursive chain, having a bottom
		// value means that + can also be rewritten as strcat. This does not
		// happen when the full chain is computed (when unwinding the results)
		// and will thus cause a diff in results due to the missing expression
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	@Ignore
	public void testLoopInsensitive() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(ContextInsensitiveToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "loop/insensitive";
		conf.programFile = "loop.imp";
		// during the first evaluation of the recursive chain, having a bottom
		// value means that + can also be rewritten as strcat. This does not
		// happen when the full chain is computed (when unwinding the results)
		// and will thus cause a diff in results due to the missing expression
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	@Ignore
	public void testInfiniteLoopKDepth() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(KDepthToken.getSingleton(5));
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "infiniteLoop/kdepth";
		conf.programFile = "infiniteLoop.imp";
		// during the first evaluation of the recursive chain, having a bottom
		// value means that + can also be rewritten as strcat. This does not
		// happen when the full chain is computed (when unwinding the results)
		// and will thus cause a diff in results due to the missing expression
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	@Ignore
	public void testInfiniteLoopLast() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(LastCallToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "infiniteLoop/last";
		conf.programFile = "infiniteLoop.imp";
		// during the first evaluation of the recursive chain, having a bottom
		// value means that + can also be rewritten as strcat. This does not
		// happen when the full chain is computed (when unwinding the results)
		// and will thus cause a diff in results due to the missing expression
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	@Ignore
	public void testInfiniteLoopInsensitive() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(ContextInsensitiveToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "infiniteLoop/insensitive";
		conf.programFile = "infiniteLoop.imp";
		// during the first evaluation of the recursive chain, having a bottom
		// value means that + can also be rewritten as strcat. This does not
		// happen when the full chain is computed (when unwinding the results)
		// and will thus cause a diff in results due to the missing expression
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	@Ignore
	public void testInfiniteDirectRecursionKDepth() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(KDepthToken.getSingleton(5));
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "infiniteDirectRecursion/kdepth";
		conf.programFile = "infiniteDirectRecursion.imp";
		// during the first evaluation of the recursive chain, having a bottom
		// value means that + can also be rewritten as strcat. This does not
		// happen when the full chain is computed (when unwinding the results)
		// and will thus cause a diff in results due to the missing expression
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	@Ignore
	public void testInfiniteDirectRecursionLast() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(LastCallToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "infiniteDirectRecursion/last";
		conf.programFile = "infiniteDirectRecursion.imp";
		// during the first evaluation of the recursive chain, having a bottom
		// value means that + can also be rewritten as strcat. This does not
		// happen when the full chain is computed (when unwinding the results)
		// and will thus cause a diff in results due to the missing expression
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	@Ignore
	public void testInfiniteDirectRecursionInsensitive() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(ContextInsensitiveToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "infiniteDirectRecursion/insensitive";
		conf.programFile = "infiniteDirectRecursion.imp";
		// during the first evaluation of the recursive chain, having a bottom
		// value means that + can also be rewritten as strcat. This does not
		// happen when the full chain is computed (when unwinding the results)
		// and will thus cause a diff in results due to the missing expression
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	public void testDirectRecursionKDepth() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(KDepthToken.getSingleton(5));
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "directRecursion/kdepth";
		conf.programFile = "directRecursion.imp";
		// during the first evaluation of the recursive chain, having a bottom
		// value means that + can also be rewritten as strcat. This does not
		// happen when the full chain is computed (when unwinding the results)
		// and will thus cause a diff in results due to the missing expression
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	public void testDirectRecursionLast() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(LastCallToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "directRecursion/last";
		conf.programFile = "directRecursion.imp";
		// during the first evaluation of the recursive chain, having a bottom
		// value means that + can also be rewritten as strcat. This does not
		// happen when the full chain is computed (when unwinding the results)
		// and will thus cause a diff in results due to the missing expression
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	public void testDirectRecursionInsensitive() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				new Interval(),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(ContextInsensitiveToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		conf.testDir = "interprocedural";
		conf.testSubDir = "directRecursion/insensitive";
		conf.programFile = "directRecursion.imp";
		// during the first evaluation of the recursive chain, having a bottom
		// value means that + can also be rewritten as strcat. This does not
		// happen when the full chain is computed (when unwinding the results)
		// and will thus cause a diff in results due to the missing expression
		conf.compareWithOptimization = false;
		perform(conf);
	}
}
