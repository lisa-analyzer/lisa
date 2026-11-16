package it.unive.lisa.analysis.heap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestAbstractDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.VoidType;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

/**
 * Tests for the default methods of {@link HeapDomain} and
 * {@link BaseHeapDomain}, exercised through a minimal fixture implementation of
 * both interfaces.
 */
public class BaseHeapDomainTest {

	private static final Type TYPE = Untyped.INSTANCE;

	private static final CodeLocation LOC = SyntheticLocation.INSTANCE;

	private static final SemanticOracle ORACLE = new TestAbstractDomain().new TestOracle();

	private static final ProgramPoint PP = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return LOC;
		}

		@Override
		public CFG getCFG() {
			return null;
		}
	};

	/**
	 * A trivial {@link HeapLattice} with just a top, a bottom, and a single
	 * non-extremal element, sufficient to drive the control-flow branches of
	 * the default methods under test.
	 */
	private static final class FakeHeapLattice
			implements
			HeapLattice<FakeHeapLattice> {

		private static final FakeHeapLattice TOP = new FakeHeapLattice("TOP");
		private static final FakeHeapLattice BOTTOM = new FakeHeapLattice("BOTTOM");
		private static final FakeHeapLattice NORMAL = new FakeHeapLattice("NORMAL");

		private final String label;

		private FakeHeapLattice(
				String label) {
			this.label = label;
		}

		@Override
		public FakeHeapLattice top() {
			return TOP;
		}

		@Override
		public FakeHeapLattice bottom() {
			return BOTTOM;
		}

		@Override
		public boolean isTop() {
			return this == TOP;
		}

		@Override
		public boolean isBottom() {
			return this == BOTTOM;
		}

		@Override
		public boolean lessOrEqual(
				FakeHeapLattice other) {
			return this == other || this == BOTTOM || other == TOP;
		}

		@Override
		public FakeHeapLattice lub(
				FakeHeapLattice other) {
			return this == other ? this : TOP;
		}

		@Override
		public List<HeapReplacement> expand(
				HeapReplacement base) {
			return List.of(base);
		}

		@Override
		public boolean knowsIdentifier(
				Identifier id) {
			return false;
		}

		@Override
		public Pair<FakeHeapLattice, List<HeapReplacement>> forgetIdentifier(
				Identifier id,
				ProgramPoint pp) {
			return Pair.of(this, List.of());
		}

		@Override
		public Pair<FakeHeapLattice, List<HeapReplacement>> forgetIdentifiers(
				Iterable<Identifier> ids,
				ProgramPoint pp) {
			return Pair.of(this, List.of());
		}

		@Override
		public Pair<FakeHeapLattice, List<HeapReplacement>> forgetIdentifiersIf(
				java.util.function.Predicate<Identifier> test,
				ProgramPoint pp) {
			return Pair.of(this, List.of());
		}

		@Override
		public Pair<FakeHeapLattice, List<HeapReplacement>> pushScope(
				it.unive.lisa.analysis.ScopeToken token,
				ProgramPoint pp) {
			return Pair.of(this, List.of());
		}

		@Override
		public Pair<FakeHeapLattice, List<HeapReplacement>> popScope(
				it.unive.lisa.analysis.ScopeToken token,
				ProgramPoint pp) {
			return Pair.of(this, List.of());
		}

		@Override
		public StructuredRepresentation representation() {
			return new StringRepresentation(label);
		}

		@Override
		public String toString() {
			return label;
		}
	}

	/**
	 * A {@link BaseHeapDomain} whose identifier-rewriting and heap-expression
	 * semantics are driven by test-supplied maps, so that the default methods
	 * of {@link HeapDomain}/{@link BaseHeapDomain} can be exercised end to end.
	 */
	private static final class FakeHeapDomain
			implements
			BaseHeapDomain<FakeHeapLattice> {

		private final Map<Identifier, ExpressionSet> rewrites = new HashMap<>();
		private final Map<HeapExpression,
				Pair<FakeHeapLattice, List<HeapReplacement>>> semantics = new IdentityHashMap<>();
		private final Set<HeapExpression> visited = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

		void mapRewrite(
				Identifier id,
				SymbolicExpression... to) {
			rewrites.put(id, new ExpressionSet(new HashSet<>(java.util.Arrays.asList(to))));
		}

		void mapSemantics(
				HeapExpression e,
				FakeHeapLattice state,
				HeapReplacement... repls) {
			semantics.put(e, Pair.of(state, java.util.Arrays.asList(repls)));
		}

		boolean wasVisited(
				HeapExpression e) {
			return visited.contains(e);
		}

		@Override
		public ExpressionSet rewriteIdentifier(
				Identifier expression,
				FakeHeapLattice state,
				ProgramPoint pp,
				SemanticOracle oracle) {
			return rewrites.getOrDefault(expression, new ExpressionSet(expression));
		}

		@Override
		public Pair<FakeHeapLattice, List<HeapReplacement>> semanticsOf(
				FakeHeapLattice state,
				HeapExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle) {
			visited.add(expression);
			return semantics.getOrDefault(expression, Pair.of(state, List.of()));
		}

		@Override
		public Pair<FakeHeapLattice, List<HeapReplacement>> assign(
				FakeHeapLattice state,
				Identifier id,
				SymbolicExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle) {
			return Pair.of(state, List.of());
		}

		@Override
		public Pair<FakeHeapLattice, List<HeapReplacement>> assume(
				FakeHeapLattice state,
				SymbolicExpression expression,
				ProgramPoint src,
				ProgramPoint dest,
				SemanticOracle oracle) {
			return Pair.of(state, List.of());
		}

		@Override
		public Satisfiability alias(
				FakeHeapLattice state,
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp,
				SemanticOracle oracle) {
			return Satisfiability.UNKNOWN;
		}

		@Override
		public Satisfiability isReachableFrom(
				FakeHeapLattice state,
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return reachableFrom(state, x, pp, oracle).elements().contains(y)
					? Satisfiability.SATISFIED
					: Satisfiability.NOT_SATISFIED;
		}

		@Override
		public FakeHeapLattice makeLattice() {
			return FakeHeapLattice.NORMAL;
		}

		@Override
		public ExpressionSet rewriteAccessChild(
				AccessChild expression,
				ExpressionSet receiver,
				ExpressionSet child,
				FakeHeapLattice state,
				ProgramPoint pp,
				SemanticOracle oracle) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ExpressionSet rewriteMemoryAllocation(
				MemoryAllocation expression,
				FakeHeapLattice state,
				ProgramPoint pp,
				SemanticOracle oracle) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ExpressionSet rewriteHeapReference(
				HeapReference expression,
				ExpressionSet arg,
				FakeHeapLattice state,
				ProgramPoint pp,
				SemanticOracle oracle) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ExpressionSet rewriteHeapDereference(
				HeapDereference expression,
				ExpressionSet arg,
				FakeHeapLattice state,
				ProgramPoint pp,
				SemanticOracle oracle) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ExpressionSet rewriteNullConstant(
				NullConstant expression,
				FakeHeapLattice state,
				ProgramPoint pp,
				SemanticOracle oracle) {
			throw new UnsupportedOperationException();
		}
	}

	private final Variable e = new Variable(TYPE, "e", LOC);

	@Test
	public void testSmallStepSemanticsOfPlainValueExpressionIsIdentity()
			throws SemanticException {
		FakeHeapDomain domain = new FakeHeapDomain();
		Pair<FakeHeapLattice, List<HeapReplacement>> res = domain.smallStepSemantics(
				FakeHeapLattice.NORMAL, e, PP, ORACLE);
		assertSame(FakeHeapLattice.NORMAL, res.getLeft());
		assertTrue(res.getRight().isEmpty());
	}

	@Test
	public void testSmallStepSemanticsUnwrapsUnaryExpressions()
			throws SemanticException {
		FakeHeapDomain domain = new FakeHeapDomain();
		UnaryExpression unary = new UnaryExpression(TYPE, e, LogicalNegation.INSTANCE, LOC);
		Pair<FakeHeapLattice, List<HeapReplacement>> res = domain.smallStepSemantics(
				FakeHeapLattice.NORMAL, unary, PP, ORACLE);
		assertSame(FakeHeapLattice.NORMAL, res.getLeft());
		assertTrue(res.getRight().isEmpty());
	}

	@Test
	public void testSmallStepSemanticsBinaryUnionsBothOperandsReplacements()
			throws SemanticException {
		FakeHeapDomain domain = new FakeHeapDomain();
		NullConstant left = new NullConstant(LOC);
		NullConstant right = new NullConstant(LOC);
		HeapReplacement replLeft = new HeapReplacement().withSource(e);
		Variable f = new Variable(TYPE, "f", LOC);
		HeapReplacement replRight = new HeapReplacement().withSource(f);
		domain.mapSemantics(left, FakeHeapLattice.NORMAL, replLeft);
		domain.mapSemantics(right, FakeHeapLattice.NORMAL, replRight);

		BinaryExpression binary = new BinaryExpression(TYPE, left, right, LogicalAnd.INSTANCE, LOC);
		Pair<FakeHeapLattice, List<HeapReplacement>> res = domain.smallStepSemantics(
				FakeHeapLattice.NORMAL, binary, PP, ORACLE);

		assertTrue(domain.wasVisited(left));
		assertTrue(domain.wasVisited(right));
		assertEquals(2, res.getRight().size());
		assertTrue(res.getRight().contains(replLeft));
		assertTrue(res.getRight().contains(replRight));
	}

	@Test
	public void testSmallStepSemanticsBinaryShortCircuitsWhenLeftIsBottom()
			throws SemanticException {
		FakeHeapDomain domain = new FakeHeapDomain();
		NullConstant left = new NullConstant(LOC);
		NullConstant right = new NullConstant(LOC);
		HeapReplacement replLeft = new HeapReplacement().withSource(e);
		domain.mapSemantics(left, FakeHeapLattice.BOTTOM, replLeft);

		BinaryExpression binary = new BinaryExpression(TYPE, left, right, LogicalAnd.INSTANCE, LOC);
		Pair<FakeHeapLattice, List<HeapReplacement>> res = domain.smallStepSemantics(
				FakeHeapLattice.NORMAL, binary, PP, ORACLE);

		assertTrue(res.getLeft().isBottom());
		assertEquals(List.of(replLeft), res.getRight());
		assertTrue(domain.wasVisited(left));
		// the right operand must not be evaluated once the left one goes to
		// bottom
		assertFalse(domain.wasVisited(right));
	}

	@Test
	public void testSmallStepSemanticsTernaryShortCircuitsWhenMiddleIsBottom()
			throws SemanticException {
		FakeHeapDomain domain = new FakeHeapDomain();
		NullConstant left = new NullConstant(LOC);
		NullConstant middle = new NullConstant(LOC);
		NullConstant right = new NullConstant(LOC);
		domain.mapSemantics(middle, FakeHeapLattice.BOTTOM);

		TernaryExpression ternary = new TernaryExpression(
				TYPE, left, middle, right, StringSubstring.INSTANCE, LOC);
		Pair<FakeHeapLattice, List<HeapReplacement>> res = domain.smallStepSemantics(
				FakeHeapLattice.NORMAL, ternary, PP, ORACLE);

		assertTrue(res.getLeft().isBottom());
		assertTrue(domain.wasVisited(left));
		assertTrue(domain.wasVisited(middle));
		assertFalse(domain.wasVisited(right));
	}

	@Test
	public void testBatchRewritePassesThroughExpressionsNotNeedingRewriting()
			throws SemanticException {
		FakeHeapDomain domain = new FakeHeapDomain();
		Constant noRewrite = new Constant(VoidType.INSTANCE, "c", LOC);
		domain.mapRewrite(e, e);

		ExpressionSet batch = new ExpressionSet(new HashSet<>(Set.of(e, noRewrite)));
		ExpressionSet result = domain.rewrite(FakeHeapLattice.NORMAL, batch, PP, ORACLE);

		assertEquals(Set.of(e, noRewrite), result.elements());
	}

	@Test
	public void testReachableFromExploresAllBranchesOfTheHeapGraph()
			throws SemanticException {
		// this test targets the default implementation of
		// HeapDomain#reachableFrom(...): it builds a heap shaped as
		//
		// e --> locA --> locC --> locF
		// \--> locB --> locE --> locG
		//
		// where the outgoing edges of "e" are resolved in a single rewrite
		// (as it would happen for a variable pointing to two possible
		// locations). A correct fixpoint computation must discover every
		// location transitively reachable from "e", including the leaves
		// locF and locG.
		FakeHeapDomain domain = new FakeHeapDomain();

		HeapLocation locA = new HeapLocation(TYPE, "locA", false, LOC);
		HeapLocation locB = new HeapLocation(TYPE, "locB", false, LOC);
		HeapLocation locC = new HeapLocation(TYPE, "locC", false, LOC);
		HeapLocation locE = new HeapLocation(TYPE, "locE", false, LOC);
		HeapLocation locF = new HeapLocation(TYPE, "locF", false, LOC);
		HeapLocation locG = new HeapLocation(TYPE, "locG", false, LOC);

		MemoryPointer pToA = new MemoryPointer(TYPE, locA, LOC);
		MemoryPointer pToB = new MemoryPointer(TYPE, locB, LOC);
		MemoryPointer pToC = new MemoryPointer(TYPE, locC, LOC);
		MemoryPointer pToE = new MemoryPointer(TYPE, locE, LOC);
		MemoryPointer pToF = new MemoryPointer(TYPE, locF, LOC);
		MemoryPointer pToG = new MemoryPointer(TYPE, locG, LOC);

		domain.mapRewrite(e, pToA, pToB);
		domain.mapRewrite(locA, pToC);
		domain.mapRewrite(locB, pToE);
		domain.mapRewrite(locC, pToF);
		domain.mapRewrite(locE, pToG);
		// locF and locG are leaves: they are left unmapped, so the default
		// rewriteIdentifier(...) returns them unchanged (not a MemoryPointer)

		ExpressionSet reachable = domain.reachableFrom(FakeHeapLattice.NORMAL, e, PP, ORACLE);

		assertTrue(
				reachable.elements().containsAll(Set.of(locA, locB, locC, locE, locF, locG)),
				"reachableFrom did not explore the whole heap graph, found: " + reachable.elements());
	}

	@Test
	public void testAreMutuallyReachableCombinesBothDirections()
			throws SemanticException {
		FakeHeapDomain domain = new FakeHeapDomain();
		Variable x = new Variable(TYPE, "x", LOC);
		Variable y = new Variable(TYPE, "y", LOC);
		// x and y do not point to each other: they rewrite to themselves, so
		// neither is reachable from the other
		Satisfiability sat = domain.areMutuallyReachable(FakeHeapLattice.NORMAL, x, y, PP, ORACLE);
		assertEquals(Satisfiability.NOT_SATISFIED, sat);
	}

}
