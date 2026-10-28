package it.unive.lisa.program.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link CFG#getMostRecentGuard(ProgramPoint)} (and its
 * {@code Loop}/{@code IfThenElse}-specific variants), which all delegate to the
 * private {@code CFG#getRecent} helper. A stale version of that helper mixed
 * the {@code -1} "unresolvable distance" sentinel returned by
 * {@link ControlFlowStructure#distance(Statement)} directly into the numeric
 * "closest so far" comparison: since {@code -1} sorts below every real
 * distance, an unresolvable structure could either be picked as the "closest"
 * one, or (once it had been) unconditionally overwrite the result with every
 * subsequently visited structure regardless of its own distance. These tests
 * use hand-built {@link ControlFlowStructure} fakes with a controlled
 * {@link ControlFlowStructure#distance(Statement)} to exercise exactly that
 * situation without needing a graph whose real BFS distance happens to fail.
 */
public class CFGGuardsTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("fake", 0, 0);

	private static CFG newCfg(
			String name) {
		ClassUnit unit = new ClassUnit(
				LOC, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "unit-" + name, false);
		CFG cfg = new CFG(new CodeMemberDescriptor(LOC, unit, false, name));
		return cfg;
	}

	// Statement#equals() is based on class + location only, and NoOp does not
	// add any further discriminating state: distinct NoOp nodes used together
	// in the same cfg must therefore be built at distinct locations, or they
	// collapse into a single node (and, for control flow structures sharing a
	// condition, into a rejected duplicate)
	private static SourceCodeLocation loc(
			int col) {
		return new SourceCodeLocation("fake", 0, col);
	}

	/**
	 * A {@link ControlFlowStructure} whose {@link #contains(Statement)} always
	 * matches the given target, and whose {@link #distance(Statement)} is fixed
	 * to whatever was passed to the constructor, bypassing the real
	 * body/graph-based computation entirely.
	 */
	private static class FakeStructure
			extends
			ControlFlowStructure {

		private final Statement target;
		private final int fixedDistance;

		FakeStructure(
				NodeList<CFG, Statement, it.unive.lisa.program.cfg.edge.Edge> matrix,
				Statement condition,
				Statement target,
				int fixedDistance) {
			super(matrix, condition, null);
			this.target = target;
			this.fixedDistance = fixedDistance;
		}

		@Override
		protected Collection<Statement> bodyStatements() {
			return Collections.singleton(target);
		}

		@Override
		public boolean contains(
				Statement st) {
			return st == target;
		}

		@Override
		public void simplify(
				Set<Statement> targets) {
			// not exercised
		}

		@Override
		public int distance(
				Statement st) {
			return st == target ? fixedDistance : -1;
		}

		@Override
		public String toString() {
			return "fake[" + getCondition() + "]";
		}

		@Override
		public Collection<Statement> getTargetedStatements() {
			return Collections.emptySet();
		}

		@Override
		public void addWith(
				Statement toAdd,
				Statement reference) {
			// not exercised
		}

		@Override
		public void replace(
				Statement original,
				Statement replacement) {
			// not exercised
		}

	}

	@Test
	public void unresolvableStructureIsIgnoredInFavorOfAResolvableOne() {
		CFG cfg = newCfg("mixed");
		Statement target = new NoOp(cfg, loc(0));
		Statement farCondition = new NoOp(cfg, loc(1));
		Statement unresolvableCondition = new NoOp(cfg, loc(2));
		cfg.addNode(target, true);
		cfg.addNode(farCondition);
		cfg.addNode(unresolvableCondition);
		cfg.addEdge(new SequentialEdge(target, farCondition));
		cfg.addEdge(new SequentialEdge(farCondition, unresolvableCondition));

		// distance -1 must never win, regardless of insertion order
		cfg.getDescriptor()
				.addControlFlowStructure(new FakeStructure(cfg.getNodeList(), unresolvableCondition, target, -1));
		cfg.getDescriptor().addControlFlowStructure(new FakeStructure(cfg.getNodeList(), farCondition, target, 5));

		assertEquals(farCondition, cfg.getMostRecentGuard(target));
	}

	@Test
	public void resolvableStructureFoundAfterAnUnresolvableOneIsNotOverwritten() {
		// same as above, but insertion order is reversed: the unresolvable
		// structure is visited BEFORE the resolvable one. The old buggy
		// implementation's "|| min == -1" clause made every later candidate
		// unconditionally win once an unresolvable one had been seen, so this
		// ordering specifically exercises that regression.
		CFG cfg = newCfg("mixed-reversed");
		Statement target = new NoOp(cfg, loc(0));
		Statement nearCondition = new NoOp(cfg, loc(1));
		Statement unresolvableCondition = new NoOp(cfg, loc(2));
		cfg.addNode(target, true);
		cfg.addNode(nearCondition);
		cfg.addNode(unresolvableCondition);
		cfg.addEdge(new SequentialEdge(target, nearCondition));
		cfg.addEdge(new SequentialEdge(nearCondition, unresolvableCondition));

		cfg.getDescriptor()
				.addControlFlowStructure(new FakeStructure(cfg.getNodeList(), unresolvableCondition, target, -1));
		cfg.getDescriptor().addControlFlowStructure(new FakeStructure(cfg.getNodeList(), nearCondition, target, 1));

		assertEquals(nearCondition, cfg.getMostRecentGuard(target));
	}

	@Test
	public void onlyUnresolvableStructuresThrow() {
		CFG cfg = newCfg("onlyUnresolvable");
		Statement target = new NoOp(cfg, loc(0));
		Statement condition = new NoOp(cfg, loc(1));
		cfg.addNode(target, true);
		cfg.addNode(condition);
		cfg.addEdge(new SequentialEdge(target, condition));

		cfg.getDescriptor().addControlFlowStructure(new FakeStructure(cfg.getNodeList(), condition, target, -1));

		assertThrows(IllegalStateException.class, () -> cfg.getMostRecentGuard(target));
	}

	@Test
	public void noContainingStructureYieldsNull() {
		CFG cfg = newCfg("none");
		Statement target = new NoOp(cfg, loc(0));
		cfg.addNode(target, true);

		assertNull(cfg.getMostRecentGuard(target));
	}

	@Test
	public void closestOfMultipleResolvableStructuresIsPicked() {
		CFG cfg = newCfg("multiple");
		Statement target = new NoOp(cfg, loc(0));
		Statement near = new NoOp(cfg, loc(1));
		Statement far = new NoOp(cfg, loc(2));
		cfg.addNode(target, true);
		cfg.addNode(near);
		cfg.addNode(far);
		cfg.addEdge(new SequentialEdge(target, near));
		cfg.addEdge(new SequentialEdge(near, far));

		cfg.getDescriptor().addControlFlowStructure(new FakeStructure(cfg.getNodeList(), far, target, 10));
		cfg.getDescriptor().addControlFlowStructure(new FakeStructure(cfg.getNodeList(), near, target, 2));

		assertEquals(near, cfg.getMostRecentGuard(target));
	}

}
