package it.unive.lisa.analysis.value;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/**
 * Tests for the default {@code canProcess(...)} implementations provided by
 * {@link BooleanAbstraction}, {@link NumericAbstraction} and
 * {@link StringAbstraction}. All three share the exact same shape of logic,
 * differing only in which {@link Type} predicate they check, so they are tested
 * together against the same fixture.
 */
public class ValueAbstractionsTest {

	private static final TestTypeSystem TYPES = new TestTypeSystem();

	private static final Type BOOLEAN = TYPES.getBooleanType();

	private static final Type NUMERIC = TYPES.getIntegerType();

	private static final Type STRING = TYPES.getStringType();

	// not a value type at all (it is an in-memory type)
	private static final Type NOT_A_VALUE_TYPE = NullType.INSTANCE;

	/**
	 * A trivial lattice, never inspected by {@code canProcess(...)}.
	 */
	private static final class Dummy
			implements
			ValueLattice<Dummy> {

		@Override
		public boolean isBottom() {
			return false;
		}

		@Override
		public boolean isTop() {
			return true;
		}

		@Override
		public Dummy top() {
			return this;
		}

		@Override
		public Dummy bottom() {
			return this;
		}

		@Override
		public boolean lessOrEqual(
				Dummy other) {
			return true;
		}

		@Override
		public Dummy lub(
				Dummy other) {
			return this;
		}

		@Override
		public boolean knowsIdentifier(
				Identifier id) {
			return false;
		}

		@Override
		public Dummy forgetIdentifier(
				Identifier id,
				ProgramPoint pp) {
			return this;
		}

		@Override
		public Dummy forgetIdentifiers(
				Iterable<Identifier> ids,
				ProgramPoint pp) {
			return this;
		}

		@Override
		public Dummy forgetIdentifiersIf(
				Predicate<Identifier> test,
				ProgramPoint pp) {
			return this;
		}

		@Override
		public Dummy pushScope(
				ScopeToken token,
				ProgramPoint pp) {
			return this;
		}

		@Override
		public Dummy popScope(
				ScopeToken token,
				ProgramPoint pp) {
			return this;
		}

		@Override
		public Dummy store(
				Identifier target,
				Identifier source) {
			return this;
		}

		@Override
		public StructuredRepresentation representation() {
			return new StringRepresentation("dummy");
		}
	}

	private static final class BoolDomain
			implements
			BooleanAbstraction<Dummy> {

		@Override
		public Dummy assign(
				Dummy state,
				Identifier id,
				ValueExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle) {
			return state;
		}

		@Override
		public Dummy smallStepSemantics(
				Dummy state,
				ValueExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle) {
			return state;
		}

		@Override
		public Dummy assume(
				Dummy state,
				ValueExpression expression,
				ProgramPoint src,
				ProgramPoint dest,
				SemanticOracle oracle) {
			return state;
		}

		@Override
		public Dummy makeLattice() {
			return new Dummy();
		}
	}

	private static final class NumDomain
			implements
			NumericAbstraction<Dummy> {

		@Override
		public Dummy assign(
				Dummy state,
				Identifier id,
				ValueExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle) {
			return state;
		}

		@Override
		public Dummy smallStepSemantics(
				Dummy state,
				ValueExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle) {
			return state;
		}

		@Override
		public Dummy assume(
				Dummy state,
				ValueExpression expression,
				ProgramPoint src,
				ProgramPoint dest,
				SemanticOracle oracle) {
			return state;
		}

		@Override
		public Dummy makeLattice() {
			return new Dummy();
		}
	}

	private static final class StrDomain
			implements
			StringAbstraction<Dummy> {

		@Override
		public Dummy assign(
				Dummy state,
				Identifier id,
				ValueExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle) {
			return state;
		}

		@Override
		public Dummy smallStepSemantics(
				Dummy state,
				ValueExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle) {
			return state;
		}

		@Override
		public Dummy assume(
				Dummy state,
				ValueExpression expression,
				ProgramPoint src,
				ProgramPoint dest,
				SemanticOracle oracle) {
			return state;
		}

		@Override
		public Dummy makeLattice() {
			return new Dummy();
		}
	}

	/**
	 * A {@link SemanticOracle} whose runtime types and whole-value-analysis
	 * flag are fully test-controlled.
	 */
	private static final class FakeOracle
			implements
			SemanticOracle {

		private final boolean whole;
		private final Set<Type> runtimeTypes;
		private final boolean throwOnQuery;

		private FakeOracle(
				boolean whole,
				Set<Type> runtimeTypes,
				boolean throwOnQuery) {
			this.whole = whole;
			this.runtimeTypes = runtimeTypes;
			this.throwOnQuery = throwOnQuery;
		}

		@Override
		public EventQueue getEventQueue() {
			return null;
		}

		@Override
		public boolean hasWholeValueAnlysis() {
			return whole;
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

	private static final ProgramPoint PP = new ProgramPoint() {

		@Override
		public it.unive.lisa.program.cfg.CodeLocation getLocation() {
			return SyntheticLocation.INSTANCE;
		}

		@Override
		public it.unive.lisa.program.cfg.CFG getCFG() {
			return null;
		}
	};

	private final Variable var = new Variable(BOOLEAN, "v", SyntheticLocation.INSTANCE);

	// ----- BooleanAbstraction -----

	@Test
	public void testBooleanCanProcessPushInvWithWholeValueAnalysisUsesStaticBooleanType()
			throws SemanticException {
		BoolDomain domain = new BoolDomain();
		PushInv boolPushInv = new PushInv(BOOLEAN, SyntheticLocation.INSTANCE);
		PushInv numPushInv = new PushInv(NUMERIC, SyntheticLocation.INSTANCE);
		FakeOracle oracle = new FakeOracle(true, Set.of(), false);
		assertTrue(domain.canProcess(boolPushInv, PP, oracle));
		assertFalse(domain.canProcess(numPushInv, PP, oracle));
	}

	@Test
	public void testBooleanCanProcessPushInvWithoutWholeValueAnalysisUsesIsValueType()
			throws SemanticException {
		BoolDomain domain = new BoolDomain();
		PushInv numPushInv = new PushInv(NUMERIC, SyntheticLocation.INSTANCE);
		PushInv notValuePushInv = new PushInv(NOT_A_VALUE_TYPE, SyntheticLocation.INSTANCE);
		FakeOracle oracle = new FakeOracle(false, Set.of(), false);
		// without whole-value analysis, any value type is accepted, even a
		// non-boolean one
		assertTrue(domain.canProcess(numPushInv, PP, oracle));
		assertFalse(domain.canProcess(notValuePushInv, PP, oracle));
	}

	@Test
	public void testBooleanCanProcessReturnsFalseWhenOracleThrows() {
		BoolDomain domain = new BoolDomain();
		FakeOracle oracle = new FakeOracle(true, Set.of(), true);
		assertFalse(domain.canProcess(var, PP, oracle));
	}

	@Test
	public void testBooleanCanProcessReturnsTrueWhenNoRuntimeTypesAreAvailable() {
		BoolDomain domain = new BoolDomain();
		FakeOracle oracleNull = new FakeOracle(true, null, false);
		FakeOracle oracleEmpty = new FakeOracle(true, Set.of(), false);
		assertTrue(domain.canProcess(var, PP, oracleNull));
		assertTrue(domain.canProcess(var, PP, oracleEmpty));
	}

	@Test
	public void testBooleanCanProcessMatchesOnlyBooleanRuntimeTypesWhenWhole() {
		BoolDomain domain = new BoolDomain();
		FakeOracle oracleBool = new FakeOracle(true, Set.of(BOOLEAN), false);
		FakeOracle oracleNum = new FakeOracle(true, Set.of(NUMERIC), false);
		assertTrue(domain.canProcess(var, PP, oracleBool));
		assertFalse(domain.canProcess(var, PP, oracleNum));
	}

	@Test
	public void testBooleanCanProcessMatchesAnyValueTypeWhenNotWhole() {
		BoolDomain domain = new BoolDomain();
		FakeOracle oracleNum = new FakeOracle(false, Set.of(NUMERIC), false);
		FakeOracle oracleNotValue = new FakeOracle(false, Set.of(NOT_A_VALUE_TYPE), false);
		assertTrue(domain.canProcess(var, PP, oracleNum));
		assertFalse(domain.canProcess(var, PP, oracleNotValue));
	}

	// ----- NumericAbstraction -----

	@Test
	public void testNumericCanProcessMatchesOnlyNumericRuntimeTypesWhenWhole() {
		NumDomain domain = new NumDomain();
		FakeOracle oracleNum = new FakeOracle(true, Set.of(NUMERIC), false);
		FakeOracle oracleBool = new FakeOracle(true, Set.of(BOOLEAN), false);
		assertTrue(domain.canProcess(var, PP, oracleNum));
		assertFalse(domain.canProcess(var, PP, oracleBool));
	}

	// ----- StringAbstraction -----

	@Test
	public void testStringCanProcessMatchesOnlyStringRuntimeTypesWhenWhole() {
		StrDomain domain = new StrDomain();
		FakeOracle oracleStr = new FakeOracle(true, Set.of(STRING), false);
		FakeOracle oracleBool = new FakeOracle(true, Set.of(BOOLEAN), false);
		assertTrue(domain.canProcess(var, PP, oracleStr));
		assertFalse(domain.canProcess(var, PP, oracleBool));
	}

}
