package it.unive.lisa.analysis.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestAbstractDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.memory.MemoryDomain.MemoryReplacement;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.memory.DynamicAccess;
import it.unive.lisa.symbolic.memory.GetAddress;
import it.unive.lisa.symbolic.memory.MemoryAllocation;
import it.unive.lisa.symbolic.memory.MemoryDereference;
import it.unive.lisa.symbolic.memory.MemoryExpression;
import it.unive.lisa.symbolic.memory.NullConstant;
import it.unive.lisa.symbolic.memory.StaticAccess;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryLocation;
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
 * Tests for the default methods of {@link MemoryDomain} and
 * {@link BaseMemoryDomain}, exercised through a minimal fixture implementation
 * of both interfaces.
 */
public class BaseMemoryDomainTest {

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
	 * A trivial {@link MemoryLattice} with just a top, a bottom, and a single
	 * non-extremal element, sufficient to drive the control-flow branches of
	 * the default methods under test.
	 */
	private static final class FakeMemoryLattice
			implements
			MemoryLattice<FakeMemoryLattice> {

		private static final FakeMemoryLattice TOP = new FakeMemoryLattice("TOP");
		private static final FakeMemoryLattice BOTTOM = new FakeMemoryLattice("BOTTOM");
		private static final FakeMemoryLattice NORMAL = new FakeMemoryLattice("NORMAL");

		private final String label;

		private FakeMemoryLattice(
				String label) {
			this.label = label;
		}

		@Override
		public FakeMemoryLattice top() {
			return TOP;
		}

		@Override
		public FakeMemoryLattice bottom() {
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
				FakeMemoryLattice other) {
			return this == other || this == BOTTOM || other == TOP;
		}

		@Override
		public FakeMemoryLattice lub(
				FakeMemoryLattice other) {
			return this == other ? this : TOP;
		}

		@Override
		public List<MemoryReplacement> expand(
				MemoryReplacement base) {
			return List.of(base);
		}

		@Override
		public boolean knowsIdentifier(
				Identifier id) {
			return false;
		}

		@Override
		public Pair<FakeMemoryLattice, List<MemoryReplacement>> forgetIdentifier(
				Identifier id,
				ProgramPoint pp) {
			return Pair.of(this, List.of());
		}

		@Override
		public Pair<FakeMemoryLattice, List<MemoryReplacement>> forgetIdentifiers(
				Iterable<Identifier> ids,
				ProgramPoint pp) {
			return Pair.of(this, List.of());
		}

		@Override
		public Pair<FakeMemoryLattice, List<MemoryReplacement>> forgetIdentifiersIf(
				java.util.function.Predicate<Identifier> test,
				ProgramPoint pp) {
			return Pair.of(this, List.of());
		}

		@Override
		public Pair<FakeMemoryLattice, List<MemoryReplacement>> pushScope(
				it.unive.lisa.analysis.ScopeToken token,
				ProgramPoint pp) {
			return Pair.of(this, List.of());
		}

		@Override
		public Pair<FakeMemoryLattice, List<MemoryReplacement>> popScope(
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
	 * A {@link BaseMemoryDomain} whose identifier-rewriting and
	 * memory-expression semantics are driven by test-supplied maps, so that the
	 * default methods of {@link MemoryDomain}/{@link BaseMemoryDomain} can be
	 * exercised end to end.
	 */
	private static final class FakeMemoryDomain
			implements
			BaseMemoryDomain<FakeMemoryLattice> {

		private final Map<Identifier, ExpressionSet> rewrites = new HashMap<>();
		private final Map<MemoryExpression,
				Pair<FakeMemoryLattice, List<MemoryReplacement>>> semantics = new IdentityHashMap<>();
		private final Set<MemoryExpression> visited = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

		void mapRewrite(
				Identifier id,
				SymbolicExpression... to) {
			rewrites.put(id, new ExpressionSet(new HashSet<>(java.util.Arrays.asList(to))));
		}

		void mapSemantics(
				MemoryExpression e,
				FakeMemoryLattice state,
				MemoryReplacement... repls) {
			semantics.put(e, Pair.of(state, java.util.Arrays.asList(repls)));
		}

		boolean wasVisited(
				MemoryExpression e) {
			return visited.contains(e);
		}

		@Override
		public ExpressionSet rewriteIdentifier(
				Identifier expression,
				FakeMemoryLattice state,
				ProgramPoint pp,
				SemanticOracle oracle) {
			return rewrites.getOrDefault(expression, new ExpressionSet(expression));
		}

		@Override
		public Pair<FakeMemoryLattice, List<MemoryReplacement>> semanticsOf(
				FakeMemoryLattice state,
				MemoryExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle) {
			visited.add(expression);
			return semantics.getOrDefault(expression, Pair.of(state, List.of()));
		}

		@Override
		public Pair<FakeMemoryLattice, List<MemoryReplacement>> assign(
				FakeMemoryLattice state,
				Identifier id,
				SymbolicExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle) {
			return Pair.of(state, List.of());
		}

		@Override
		public Pair<FakeMemoryLattice, List<MemoryReplacement>> assume(
				FakeMemoryLattice state,
				SymbolicExpression expression,
				ProgramPoint src,
				ProgramPoint dest,
				SemanticOracle oracle) {
			return Pair.of(state, List.of());
		}

		@Override
		public Satisfiability alias(
				FakeMemoryLattice state,
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp,
				SemanticOracle oracle) {
			return Satisfiability.UNKNOWN;
		}

		@Override
		public Satisfiability isReachableFrom(
				FakeMemoryLattice state,
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
		public FakeMemoryLattice makeLattice() {
			return FakeMemoryLattice.NORMAL;
		}

		@Override
		public ExpressionSet rewriteStaticAccess(
				StaticAccess expression,
				ExpressionSet receiver,
				Variable child,
				FakeMemoryLattice state,
				ProgramPoint pp,
				SemanticOracle oracle) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ExpressionSet rewriteDynamicAccess(
				DynamicAccess expression,
				ExpressionSet receiver,
				ExpressionSet child,
				FakeMemoryLattice state,
				ProgramPoint pp,
				SemanticOracle oracle) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ExpressionSet rewriteMemoryAllocation(
				MemoryAllocation expression,
				FakeMemoryLattice state,
				ProgramPoint pp,
				SemanticOracle oracle) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ExpressionSet rewriteGetAddress(
				GetAddress expression,
				ExpressionSet arg,
				FakeMemoryLattice state,
				ProgramPoint pp,
				SemanticOracle oracle) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ExpressionSet rewriteMemoryDereference(
				MemoryDereference expression,
				ExpressionSet arg,
				FakeMemoryLattice state,
				ProgramPoint pp,
				SemanticOracle oracle) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ExpressionSet rewriteNullConstant(
				NullConstant expression,
				FakeMemoryLattice state,
				ProgramPoint pp,
				SemanticOracle oracle) {
			throw new UnsupportedOperationException();
		}
	}

	private final Variable e = new Variable(TYPE, "e", LOC);

	@Test
	public void testSmallStepSemanticsOfPlainValueExpressionIsIdentity()
			throws SemanticException {
		FakeMemoryDomain domain = new FakeMemoryDomain();
		Pair<FakeMemoryLattice, List<MemoryReplacement>> res = domain.smallStepSemantics(
				FakeMemoryLattice.NORMAL, e, PP, ORACLE);
		assertSame(FakeMemoryLattice.NORMAL, res.getLeft());
		assertTrue(res.getRight().isEmpty());
	}

	@Test
	public void testSmallStepSemanticsUnwrapsUnaryExpressions()
			throws SemanticException {
		FakeMemoryDomain domain = new FakeMemoryDomain();
		UnaryExpression unary = new UnaryExpression(TYPE, e, LogicalNegation.INSTANCE, LOC);
		Pair<FakeMemoryLattice, List<MemoryReplacement>> res = domain.smallStepSemantics(
				FakeMemoryLattice.NORMAL, unary, PP, ORACLE);
		assertSame(FakeMemoryLattice.NORMAL, res.getLeft());
		assertTrue(res.getRight().isEmpty());
	}

	@Test
	public void testSmallStepSemanticsBinaryUnionsBothOperandsReplacements()
			throws SemanticException {
		FakeMemoryDomain domain = new FakeMemoryDomain();
		NullConstant left = new NullConstant(LOC);
		NullConstant right = new NullConstant(LOC);
		MemoryReplacement replLeft = new MemoryReplacement().withSource(e);
		Variable f = new Variable(TYPE, "f", LOC);
		MemoryReplacement replRight = new MemoryReplacement().withSource(f);
		domain.mapSemantics(left, FakeMemoryLattice.NORMAL, replLeft);
		domain.mapSemantics(right, FakeMemoryLattice.NORMAL, replRight);

		BinaryExpression binary = new BinaryExpression(TYPE, left, right, LogicalAnd.INSTANCE, LOC);
		Pair<FakeMemoryLattice, List<MemoryReplacement>> res = domain.smallStepSemantics(
				FakeMemoryLattice.NORMAL, binary, PP, ORACLE);

		assertTrue(domain.wasVisited(left));
		assertTrue(domain.wasVisited(right));
		assertEquals(2, res.getRight().size());
		assertTrue(res.getRight().contains(replLeft));
		assertTrue(res.getRight().contains(replRight));
	}

	@Test
	public void testSmallStepSemanticsBinaryShortCircuitsWhenLeftIsBottom()
			throws SemanticException {
		FakeMemoryDomain domain = new FakeMemoryDomain();
		NullConstant left = new NullConstant(LOC);
		NullConstant right = new NullConstant(LOC);
		MemoryReplacement replLeft = new MemoryReplacement().withSource(e);
		domain.mapSemantics(left, FakeMemoryLattice.BOTTOM, replLeft);

		BinaryExpression binary = new BinaryExpression(TYPE, left, right, LogicalAnd.INSTANCE, LOC);
		Pair<FakeMemoryLattice, List<MemoryReplacement>> res = domain.smallStepSemantics(
				FakeMemoryLattice.NORMAL, binary, PP, ORACLE);

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
		FakeMemoryDomain domain = new FakeMemoryDomain();
		NullConstant left = new NullConstant(LOC);
		NullConstant middle = new NullConstant(LOC);
		NullConstant right = new NullConstant(LOC);
		domain.mapSemantics(middle, FakeMemoryLattice.BOTTOM);

		TernaryExpression ternary = new TernaryExpression(
				TYPE, left, middle, right, StringSubstring.INSTANCE, LOC);
		Pair<FakeMemoryLattice, List<MemoryReplacement>> res = domain.smallStepSemantics(
				FakeMemoryLattice.NORMAL, ternary, PP, ORACLE);

		assertTrue(res.getLeft().isBottom());
		assertTrue(domain.wasVisited(left));
		assertTrue(domain.wasVisited(middle));
		assertFalse(domain.wasVisited(right));
	}

	@Test
	public void testBatchRewritePassesThroughExpressionsNotNeedingRewriting()
			throws SemanticException {
		FakeMemoryDomain domain = new FakeMemoryDomain();
		Constant noRewrite = new Constant(VoidType.INSTANCE, "c", LOC);
		domain.mapRewrite(e, e);

		ExpressionSet batch = new ExpressionSet(new HashSet<>(Set.of(e, noRewrite)));
		ExpressionSet result = domain.rewrite(FakeMemoryLattice.NORMAL, batch, PP, ORACLE);

		assertEquals(Set.of(e, noRewrite), result.elements());
	}

	@Test
	public void testReachableFromExploresAllBranchesOfTheHeapGraph()
			throws SemanticException {
		// this test targets the default implementation of
		// MemoryDomain#reachableFrom(...): it builds a heap shaped as
		//
		// e --> locA --> locC --> locF
		// \--> locB --> locE --> locG
		//
		// where the outgoing edges of "e" are resolved in a single rewrite
		// (as it would happen for a variable pointing to two possible
		// locations). A correct fixpoint computation must discover every
		// location transitively reachable from "e", including the leaves
		// locF and locG.
		FakeMemoryDomain domain = new FakeMemoryDomain();

		MemoryLocation locA = new MemoryLocation(TYPE, "locA", false, LOC);
		MemoryLocation locB = new MemoryLocation(TYPE, "locB", false, LOC);
		MemoryLocation locC = new MemoryLocation(TYPE, "locC", false, LOC);
		MemoryLocation locE = new MemoryLocation(TYPE, "locE", false, LOC);
		MemoryLocation locF = new MemoryLocation(TYPE, "locF", false, LOC);
		MemoryLocation locG = new MemoryLocation(TYPE, "locG", false, LOC);

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

		ExpressionSet reachable = domain.reachableFrom(FakeMemoryLattice.NORMAL, e, PP, ORACLE);

		assertTrue(
				reachable.elements().containsAll(Set.of(locA, locB, locC, locE, locF, locG)),
				"reachableFrom did not explore the whole heap graph, found: " + reachable.elements());
	}

	@Test
	public void testAreMutuallyReachableCombinesBothDirections()
			throws SemanticException {
		FakeMemoryDomain domain = new FakeMemoryDomain();
		Variable x = new Variable(TYPE, "x", LOC);
		Variable y = new Variable(TYPE, "y", LOC);
		// x and y do not point to each other: they rewrite to themselves, so
		// neither is reachable from the other
		Satisfiability sat = domain.areMutuallyReachable(FakeMemoryLattice.NORMAL, x, y, PP, ORACLE);
		assertEquals(Satisfiability.NOT_SATISFIED, sat);
	}

}
