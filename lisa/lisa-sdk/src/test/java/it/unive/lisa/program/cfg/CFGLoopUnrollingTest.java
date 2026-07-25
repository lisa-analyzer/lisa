package it.unive.lisa.program.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.controlFlow.Loop;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.transform.LoopUnrolling;
import it.unive.lisa.program.cfg.transform.UnrolledLocation;
import java.util.Collection;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LoopUnrolling} and {@link UnrolledLocation}.
 *
 * @author <a href="mailto:giacomo12596@gmail.com">Giacomo Zanatta</a>
 */
public class CFGLoopUnrollingTest {

	private static final ClassUnit unit = new ClassUnit(
			new SourceCodeLocation("unknown", 0, 0),
			new Program(new TestLanguageFeatures(), new TestTypeSystem()),
			"Testing",
			false);

	@Test
	public void testUnrolledLocationEqualsAndHashCode() {
		SourceCodeLocation loc = new SourceCodeLocation("f", 1, 1);
		UnrolledLocation a1 = new UnrolledLocation(loc, 1);
		UnrolledLocation a1bis = new UnrolledLocation(loc, 1);
		UnrolledLocation a2 = new UnrolledLocation(loc, 2);

		assertEquals(a1, a1bis, "clones with the same original+iteration should be equals");
		assertEquals(a1.hashCode(), a1bis.hashCode(), "equals implies equal hashCode");
		assertNotEquals(a1, a2, "different iterations must not be equal");
		assertNotEquals(a1, loc, "an UnrolledLocation is never equal to its plain original");
	}

	@Test
	public void testUnrolledLocationDelegatesGetCodeLocation() {
		SourceCodeLocation loc = new SourceCodeLocation("f", 3, 7);
		UnrolledLocation wrap = new UnrolledLocation(loc, 4);
		assertEquals(loc.getCodeLocation(), wrap.getCodeLocation(),
				"getCodeLocation() must delegate to the wrapped original");
	}

	@Test
	public void testUnrolledLocationCompareTo() {
		SourceCodeLocation loc = new SourceCodeLocation("f", 1, 1);
		UnrolledLocation a1 = new UnrolledLocation(loc, 1);
		UnrolledLocation a2 = new UnrolledLocation(loc, 2);

		assertTrue(a1.compareTo(a2) < 0, "iteration 1 should sort before iteration 2");
		assertTrue(a2.compareTo(a1) > 0, "iteration 2 should sort after iteration 1");
		assertEquals(0, a1.compareTo(new UnrolledLocation(loc, 1)),
				"same original+iteration should compare equal");
		// mixed-kind ordering: an UnrolledLocation ordered right after its
		// original
		assertTrue(a1.compareTo(loc) > 0,
				"UnrolledLocation should sort strictly after its plain original");
	}

	@Test
	public void testUnrolledLocationFlattensNestedWraps() {
		SourceCodeLocation loc = new SourceCodeLocation("f", 5, 5);
		UnrolledLocation inner = new UnrolledLocation(loc, 2);
		UnrolledLocation outer = new UnrolledLocation(inner, 3);
		assertSame(loc, outer.getOriginal(),
				"wrapping an UnrolledLocation should flatten to the deepest original");
		assertEquals(3, outer.getIteration(), "outer iteration must be preserved");
	}

	@Test
	public void testUnrolledLocationConstructorArgumentValidation() {
		SourceCodeLocation loc = new SourceCodeLocation("f", 5, 5);
		assertThrows(IllegalArgumentException.class,
				() -> new UnrolledLocation(null, 1),
				"null original must be rejected");
		assertThrows(IllegalArgumentException.class,
				() -> new UnrolledLocation(loc, 0),
				"iteration < 1 must be rejected");
		assertThrows(IllegalArgumentException.class,
				() -> new UnrolledLocation(loc, -1),
				"negative iteration must be rejected");
	}

	@Test
	public void testStatementCloneDeepCopiesChildren() {
		SourceCodeLocation loc = new SourceCodeLocation("f", 1, 1);
		CFG cfg = new CFG(new CodeMemberDescriptor(loc, unit, false, "cloneMe"));
		VariableRef lhs = new VariableRef(cfg, loc, "x");
		VariableRef rhs = new VariableRef(cfg, loc, "y");
		Assignment orig = new Assignment(cfg, loc, lhs, rhs);

		UnrolledLocation newLoc = new UnrolledLocation(loc, 1);
		Assignment clone = (Assignment) orig.clone(s -> new UnrolledLocation(s.getLocation(), 1));

		assertNotSame(orig, clone, "clone must be a distinct Java object");
		assertEquals(newLoc, clone.getLocation(), "clone must carry the requested new location");
		assertNotSame(orig.getLeft(), clone.getLeft(), "left child must be deep-cloned");
		assertNotSame(orig.getRight(), clone.getRight(), "right child must be deep-cloned");
		assertEquals(newLoc, clone.getLeft().getLocation(),
				"cloned children carry an UnrolledLocation wrapping their own original location");
		assertEquals(newLoc, clone.getRight().getLocation(),
				"cloned children inherit the new location");
	}

	/**
	 * Builds a minimal partial CFG mirroring the shape produced by the IMP
	 * frontend for a single {@code while(cond) { body }} loop:
	 *
	 * <pre>
	 * cond --T-> body --seq-> cond (back-edge)
	 *      --F-> follower --seq-> ret
	 * </pre>
	 *
	 * The Loop metadata is attached to the descriptor so
	 * {@link LoopUnrolling#transform(CFG)} can pick it up.
	 */
	private static CFG buildSimpleLoop(
			String methodName) {
		SourceCodeLocation condLoc = new SourceCodeLocation("f", 2, 1);
		SourceCodeLocation bodyLoc = new SourceCodeLocation("f", 3, 1);
		SourceCodeLocation followerLoc = new SourceCodeLocation("f", 4, 1);
		SourceCodeLocation retLoc = new SourceCodeLocation("f", 5, 1);
		SourceCodeLocation methodLoc = new SourceCodeLocation("f", 1, 1);

		CFG cfg = new CFG(new CodeMemberDescriptor(methodLoc, unit, false, methodName));
		VariableRef condition = new VariableRef(cfg, condLoc, "b");
		Assignment body = new Assignment(cfg, bodyLoc,
				new VariableRef(cfg, bodyLoc, "x"),
				new VariableRef(cfg, bodyLoc, "a"));
		NoOp follower = new NoOp(cfg, followerLoc);
		Return ret = new Return(cfg, retLoc, new VariableRef(cfg, retLoc, "x"));

		cfg.addNode(condition, true);
		cfg.addNode(body);
		cfg.addNode(follower);
		cfg.addNode(ret);

		cfg.addEdge(new TrueEdge(condition, body));
		cfg.addEdge(new SequentialEdge(body, condition));
		cfg.addEdge(new FalseEdge(condition, follower));
		cfg.addEdge(new SequentialEdge(follower, ret));

		Collection<Statement> bodyStatements = new HashSet<>();
		bodyStatements.add(body);
		cfg.getDescriptor().addControlFlowStructure(
				new Loop(cfg.getNodeList(), condition, follower, bodyStatements));
		return cfg;
	}

	@Test
	public void testFactorZeroIsNoop() {
		CFG cfg = buildSimpleLoop("noop");
		int nodesBefore = cfg.getNodesCount();
		int edgesBefore = cfg.getEdgesCount();
		new LoopUnrolling(0).transform(cfg);
		assertEquals(nodesBefore, cfg.getNodesCount(), "factor=0 must leave nodes unchanged");
		assertEquals(edgesBefore, cfg.getEdgesCount(), "factor=0 must leave edges unchanged");
	}

	@Test
	public void testPartialUnrollFactor2() {
		CFG cfg = buildSimpleLoop("unroll2");
		int bodySizeBefore = cfg.getDescriptor().getControlFlowStructures().stream()
				.filter(cfs -> cfs instanceof Loop)
				.mapToInt(cfs -> ((Loop) cfs).getBody().size())
				.sum();
		assertEquals(1, bodySizeBefore, "sanity: simple loop should have exactly one body statement");

		new LoopUnrolling(2).transform(cfg);

		// residual still holds the loop metadata; the unwound clones do NOT.
		Collection<Loop> loops = new HashSet<>();
		for (ControlFlowStructure cfs : cfg.getDescriptor().getControlFlowStructures())
			if (cfs instanceof Loop)
				loops.add((Loop) cfs);
		assertEquals(1, loops.size(),
				"exactly one Loop structure should remain (the residual)");

		Loop residual = loops.iterator().next();
		assertEquals(1, residual.getBody().size(),
				"the residual loop's body should still contain exactly the original body statement");

		// each unwound clone carries an UnrolledLocation with iteration in
		// {1, 2}, wrapping the original condition/body location.
		int unrolledNodes = 0;
		for (Statement s : cfg.getNodes())
			if (s.getLocation() instanceof UnrolledLocation) {
				UnrolledLocation ul = (UnrolledLocation) s.getLocation();
				assertTrue(ul.getIteration() == 1 || ul.getIteration() == 2,
						"iteration index must be in {1, 2}, got " + ul.getIteration());
				unrolledNodes++;
			}
		// 2 unwound iterations * 2 body positions (condition + body statement)
		assertEquals(4, unrolledNodes,
				"there should be 4 unwound statements (2 conditions + 2 body clones)");

		// getCycleEntries reads Loop structures; only the residual condition
		// should appear.
		Collection<Statement> entries = cfg.getCycleEntries();
		assertEquals(1, entries.size(),
				"only the residual condition should be a cycle entry");
		assertSame(residual.getCondition(), entries.iterator().next(),
				"the cycle entry must be the residual condition");
	}

	@Test
	public void testPartialUnrollFactor1() {
		CFG cfg = buildSimpleLoop("unroll1");
		new LoopUnrolling(1).transform(cfg);

		int unrolledNodes = 0;
		for (Statement s : cfg.getNodes())
			if (s.getLocation() instanceof UnrolledLocation)
				unrolledNodes++;
		// 1 unwound iteration * (1 condition + 1 body statement)
		assertEquals(2, unrolledNodes,
				"factor=1 should introduce 2 unwound clones");
	}

	@Test
	public void testCFGWithNoLoopsIsUntouched() {
		// A CFG without loops must be a no-op.
		SourceCodeLocation loc = new SourceCodeLocation("f", 1, 1);
		CFG cfg = new CFG(new CodeMemberDescriptor(loc, unit, false, "noLoops"));
		NoOp only = new NoOp(cfg, loc);
		cfg.addNode(only, true);

		int nodesBefore = cfg.getNodesCount();
		int structsBefore = cfg.getDescriptor().getControlFlowStructures().size();
		new LoopUnrolling(4).transform(cfg);
		assertEquals(nodesBefore, cfg.getNodesCount(), "no-loop CFG must be unchanged");
		assertEquals(structsBefore, cfg.getDescriptor().getControlFlowStructures().size(),
				"no-loop CFG must not gain any structures");
	}

	@Test
	public void testNegativeFactorRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> new LoopUnrolling(-1),
				"negative factor must be rejected");
	}

}
