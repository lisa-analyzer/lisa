package it.unive.lisa.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.SingleHeapLattice;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class NoOpHeapTest {

	private static final ProgramPoint fake = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return null;
		}

		@Override
		public CFG getCFG() {
			return null;
		}

	};

	private final Variable x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private final NoOpHeap heap = new NoOpHeap();

	@Test
	public void testMakeLatticeYieldsTheSingleton() {
		assertSame(SingleHeapLattice.SINGLETON, heap.makeLattice());
	}

	@Test
	public void testAssignLeavesStateUntouchedAndProducesNoSubstitutions()
			throws SemanticException {
		var res = heap.assign(SingleHeapLattice.SINGLETON, x, x, fake, null);
		assertSame(SingleHeapLattice.SINGLETON, res.getLeft());
		assertTrue(res.getRight().isEmpty());
	}

	@Test
	public void testSmallStepSemanticsLeavesStateUntouchedAndProducesNoSubstitutions()
			throws SemanticException {
		var res = heap.smallStepSemantics(SingleHeapLattice.SINGLETON, x, fake, null);
		assertSame(SingleHeapLattice.SINGLETON, res.getLeft());
		assertTrue(res.getRight().isEmpty());
	}

	@Test
	public void testAssumeLeavesStateUntouchedAndProducesNoSubstitutions()
			throws SemanticException {
		var res = heap.assume(SingleHeapLattice.SINGLETON, x, fake, fake, null);
		assertSame(SingleHeapLattice.SINGLETON, res.getLeft());
		assertTrue(res.getRight().isEmpty());
	}

	@Test
	public void testAliasAndReachabilityAreAlwaysUnknown()
			throws SemanticException {
		assertEquals(Satisfiability.UNKNOWN, heap.alias(SingleHeapLattice.SINGLETON, x, x, fake, null));
		assertEquals(Satisfiability.UNKNOWN, heap.isReachableFrom(SingleHeapLattice.SINGLETON, x, x, fake, null));
	}

	@Test
	public void testRewriteWrapsTheInputExpressionUnchanged()
			throws SemanticException {
		ExpressionSet rewritten = heap.rewrite(SingleHeapLattice.SINGLETON, x, fake, null);
		assertEquals(new ExpressionSet(x), rewritten);
	}

}
