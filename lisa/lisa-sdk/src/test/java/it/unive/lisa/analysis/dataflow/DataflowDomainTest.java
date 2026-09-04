package it.unive.lisa.analysis.dataflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DataflowDomain}: the {@code canProcess(...)} default logic,
 * and the gen/kill dispatch performed by {@code assign(...)} and
 * {@code smallStepSemantics(...)}.
 */
public class DataflowDomainTest {

	private static final ProgramPoint PP = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return SyntheticLocation.INSTANCE;
		}

		@Override
		public CFG getCFG() {
			return null;
		}
	};

	private static final class FakeOracle
			implements
			SemanticOracle {

		private final Set<Type> runtimeTypes;
		private final boolean throwOnQuery;

		private FakeOracle(
				Set<Type> runtimeTypes,
				boolean throwOnQuery) {
			this.runtimeTypes = runtimeTypes;
			this.throwOnQuery = throwOnQuery;
		}

		@Override
		public EventQueue getEventQueue() {
			return null;
		}

		@Override
		public boolean hasWholeValueAnlysis() {
			return false;
		}

		@Override
		public Set<BinaryExpression> constraints(
				it.unive.lisa.analysis.value.ValueDomain<?> requesting,
				ValueExpression e,
				ProgramPoint pp) {
			return Set.of();
		}

		@Override
		public Set<Type> getRuntimeTypesOf(
				SymbolicExpression e,
				ProgramPoint pp)
				throws SemanticException {
			if (throwOnQuery)
				throw new SemanticException("boom");
			return runtimeTypes;
		}

		@Override
		public Type getDynamicTypeOf(
				SymbolicExpression e,
				ProgramPoint pp) {
			return null;
		}

		@Override
		public ExpressionSet rewrite(
				SymbolicExpression expression,
				ProgramPoint pp) {
			return new ExpressionSet(expression);
		}

		@Override
		public ExpressionSet rewrite(
				ExpressionSet expressions,
				ProgramPoint pp) {
			return expressions;
		}

		@Override
		public Satisfiability alias(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp) {
			return Satisfiability.UNKNOWN;
		}

		@Override
		public ExpressionSet reachableFrom(
				SymbolicExpression e,
				ProgramPoint pp) {
			return new ExpressionSet(e);
		}

		@Override
		public Satisfiability isReachableFrom(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp) {
			return Satisfiability.UNKNOWN;
		}

		@Override
		public Satisfiability areMutuallyReachable(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp) {
			return Satisfiability.UNKNOWN;
		}
	}

	private static final class FakeDataflowDomain
			extends
			DataflowDomain<DefiniteSet<FakeElement>, FakeElement> {

		private Boolean canProcessOverride;
		private Set<FakeElement> genResult = Set.of();
		private Set<FakeElement> killResult = Set.of();
		private final List<String> calls = new ArrayList<>();

		@Override
		public boolean canProcess(
				ValueExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle) {
			if (canProcessOverride != null)
				return canProcessOverride;
			return super.canProcess(expression, pp, oracle);
		}

		@Override
		public Set<FakeElement> gen(
				DefiniteSet<FakeElement> state,
				Identifier id,
				ValueExpression expression,
				ProgramPoint pp) {
			calls.add("gen-assign");
			return genResult;
		}

		@Override
		public Set<FakeElement> gen(
				DefiniteSet<FakeElement> state,
				ValueExpression expression,
				ProgramPoint pp) {
			calls.add("gen-eval");
			return genResult;
		}

		@Override
		public Set<FakeElement> kill(
				DefiniteSet<FakeElement> state,
				Identifier id,
				ValueExpression expression,
				ProgramPoint pp) {
			calls.add("kill-assign");
			return killResult;
		}

		@Override
		public Set<FakeElement> kill(
				DefiniteSet<FakeElement> state,
				ValueExpression expression,
				ProgramPoint pp) {
			calls.add("kill-eval");
			return killResult;
		}

		@Override
		public DefiniteSet<FakeElement> makeLattice() {
			return new DefiniteSet<>();
		}
	}

	private final Variable x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private final FakeElement e1 = new FakeElement("e1", x);
	private final FakeElement e2 = new FakeElement("e2", x);

	// ----- canProcess -----

	@Test
	public void testCanProcessPushInvUsesStaticIsValueType() {
		FakeDataflowDomain domain = new FakeDataflowDomain();
		PushInv valueType = new PushInv(Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		PushInv notValueType = new PushInv(NullType.INSTANCE, SyntheticLocation.INSTANCE);
		FakeOracle oracle = new FakeOracle(Set.of(), false);
		assertTrue(domain.canProcess(valueType, PP, oracle));
		assertFalse(domain.canProcess(notValueType, PP, oracle));
	}

	@Test
	public void testCanProcessReturnsFalseWhenOracleThrows() {
		FakeDataflowDomain domain = new FakeDataflowDomain();
		FakeOracle oracle = new FakeOracle(Set.of(), true);
		assertFalse(domain.canProcess(x, PP, oracle));
	}

	@Test
	public void testCanProcessReturnsTrueWhenNoRuntimeTypesAreAvailable() {
		FakeDataflowDomain domain = new FakeDataflowDomain();
		FakeOracle oracle = new FakeOracle(Set.of(), false);
		assertTrue(domain.canProcess(x, PP, oracle));
	}

	@Test
	public void testCanProcessMatchesAnyValueRuntimeType() {
		FakeDataflowDomain domain = new FakeDataflowDomain();
		FakeOracle valueOracle = new FakeOracle(Set.of(Untyped.INSTANCE), false);
		FakeOracle notValueOracle = new FakeOracle(Set.of(NullType.INSTANCE), false);
		assertTrue(domain.canProcess(x, PP, valueOracle));
		assertFalse(domain.canProcess(x, PP, notValueOracle));
	}

	// ----- assign / smallStepSemantics dispatch -----

	@Test
	public void testAssignReturnsStateUnchangedWhenExpressionCannotBeProcessed()
			throws SemanticException {
		FakeDataflowDomain domain = new FakeDataflowDomain();
		domain.canProcessOverride = false;
		domain.genResult = Set.of(e2);
		DefiniteSet<FakeElement> state = new DefiniteSet<>(new HashSet<>(Set.of(e1)));

		DefiniteSet<FakeElement> result = domain.assign(state, x, x, PP, null);

		assertSame(state, result);
		assertTrue(domain.calls.isEmpty(), "gen/kill must not be invoked when the expression cannot be processed");
	}

	@Test
	public void testAssignReturnsStateUnchangedWhenStateIsBottom()
			throws SemanticException {
		FakeDataflowDomain domain = new FakeDataflowDomain();
		domain.canProcessOverride = true;
		DefiniteSet<FakeElement> bottom = new DefiniteSet<>(false);

		DefiniteSet<FakeElement> result = domain.assign(bottom, x, x, PP, null);

		assertSame(bottom, result);
		assertTrue(domain.calls.isEmpty(), "gen/kill must not be invoked on a bottom state");
	}

	@Test
	public void testAssignAppliesGenAndKillThroughTheAssigningOverloads()
			throws SemanticException {
		FakeDataflowDomain domain = new FakeDataflowDomain();
		domain.canProcessOverride = true;
		domain.genResult = Set.of(e2);
		domain.killResult = Set.of(e1);
		DefiniteSet<FakeElement> state = new DefiniteSet<>(new HashSet<>(Set.of(e1)));

		DefiniteSet<FakeElement> result = domain.assign(state, x, x, PP, null);

		assertEquals(Set.of(e2), result.getDataflowElements());
		// both the gen and the kill sets must be computed from the
		// pre-assignment state; the order in which they are computed is an
		// implementation detail, not a contractual guarantee
		assertEquals(Set.of("gen-assign", "kill-assign"), Set.copyOf(domain.calls));
		assertEquals(2, domain.calls.size());
	}

	@Test
	public void testSmallStepSemanticsAppliesGenAndKillThroughTheNonAssigningOverloads()
			throws SemanticException {
		FakeDataflowDomain domain = new FakeDataflowDomain();
		domain.canProcessOverride = true;
		domain.genResult = Set.of(e2);
		domain.killResult = Set.of(e1);
		DefiniteSet<FakeElement> state = new DefiniteSet<>(new HashSet<>(Set.of(e1)));

		DefiniteSet<FakeElement> result = domain.smallStepSemantics(state, x, PP, null);

		assertEquals(Set.of(e2), result.getDataflowElements());
		assertEquals(Set.of("gen-eval", "kill-eval"), Set.copyOf(domain.calls));
		assertEquals(2, domain.calls.size());
	}

	@Test
	public void testAssumeIsAlwaysAnIdentity()
			throws SemanticException {
		FakeDataflowDomain domain = new FakeDataflowDomain();
		DefiniteSet<FakeElement> state = new DefiniteSet<>(new HashSet<>(Set.of(e1)));
		assertSame(state, domain.assume(state, x, PP, PP, null));
	}

}
