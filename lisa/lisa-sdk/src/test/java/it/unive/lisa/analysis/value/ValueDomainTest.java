package it.unive.lisa.analysis.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/**
 * Tests for the static helpers and the default {@code constraints(...)} method
 * of {@link ValueDomain}.
 */
public class ValueDomainTest {

	private static final ClassUnit unit = new ClassUnit(
			new SourceCodeLocation("unknown", 0, 0),
			new Program(new TestLanguageFeatures(), new TestTypeSystem()),
			"Testing",
			false);

	private static ProgramPoint pp() {
		SourceCodeLocation loc = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CodeMemberDescriptor(loc, unit, false, "m"));
		VariableRef ref = new VariableRef(cfg, loc, "x");
		Return ret = new Return(cfg, loc, ref);
		cfg.addNode(ret, true);
		return ret;
	}

	private final Variable x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);

	@Test
	public void testMakeConstraintBuildsAConstantOnTheLeftAndTheExpressionOnTheRight()
			throws SemanticException {
		ProgramPoint pp = pp();
		Set<BinaryExpression> constraints = ValueDomain.makeConstraint(
				Untyped.INSTANCE, 5, ComparisonEq.INSTANCE, x, pp);

		assertEquals(1, constraints.size());
		BinaryExpression c = constraints.iterator().next();
		assertTrue(c.getLeft() instanceof Constant);
		assertEquals(5, ((Constant) c.getLeft()).getValue());
		assertSame(x, c.getRight());
		assertSame(ComparisonEq.INSTANCE, c.getOperator());
		// TestTypeSystem#getBooleanType() is not a singleton, so we can only
		// check that the constraint carries a boolean static type, not that it
		// is reference-equal to a freshly-obtained one
		assertTrue(c.getStaticType().isBooleanType());
	}

	@Test
	public void testMakeEqConstraintDelegatesToMakeConstraintWithComparisonEq()
			throws SemanticException {
		ProgramPoint pp = pp();
		Set<BinaryExpression> constraints = ValueDomain.makeEqConstraint(Untyped.INSTANCE, 42, x, pp);
		assertEquals(1, constraints.size());
		assertSame(ComparisonEq.INSTANCE, constraints.iterator().next().getOperator());
	}

	@Test
	public void testMakeRangeConstraintsWithBothBoundsNullIsEmpty()
			throws SemanticException {
		Set<BinaryExpression> constraints = ValueDomain.makeRangeConstraints(
				Untyped.INSTANCE, null, null, x, pp());
		assertTrue(constraints.isEmpty());
	}

	@Test
	public void testMakeRangeConstraintsWithOnlyLowGeneratesLowerBoundOnly()
			throws SemanticException {
		Set<BinaryExpression> constraints = ValueDomain.makeRangeConstraints(
				Untyped.INSTANCE, 0, null, x, pp());
		assertEquals(1, constraints.size());
		assertSame(ComparisonGe.INSTANCE, constraints.iterator().next().getOperator());
	}

	@Test
	public void testMakeRangeConstraintsWithOnlyHighGeneratesUpperBoundOnly()
			throws SemanticException {
		Set<BinaryExpression> constraints = ValueDomain.makeRangeConstraints(
				Untyped.INSTANCE, null, 10, x, pp());
		assertEquals(1, constraints.size());
		assertSame(ComparisonLe.INSTANCE, constraints.iterator().next().getOperator());
	}

	@Test
	public void testMakeRangeConstraintsWithBothBoundsGeneratesTwoConstraints()
			throws SemanticException {
		Set<BinaryExpression> constraints = ValueDomain.makeRangeConstraints(
				Untyped.INSTANCE, 0, 10, x, pp());
		assertEquals(2, constraints.size());
		assertEquals(
				1,
				constraints.stream().filter(c -> c.getOperator() == ComparisonGe.INSTANCE).count());
		assertEquals(
				1,
				constraints.stream().filter(c -> c.getOperator() == ComparisonLe.INSTANCE).count());
	}

	/**
	 * A minimal {@link ValueLattice} whose bottom-ness can be toggled, used to
	 * exercise the default
	 * {@link ValueDomain#constraints(ValueDomain, ValueLattice, ValueExpression, ProgramPoint, SemanticOracle)}
	 * implementation.
	 */
	private static final class FakeLattice
			implements
			ValueLattice<FakeLattice> {

		private final boolean bottom;

		private FakeLattice(
				boolean bottom) {
			this.bottom = bottom;
		}

		@Override
		public boolean isBottom() {
			return bottom;
		}

		@Override
		public boolean isTop() {
			return false;
		}

		@Override
		public FakeLattice top() {
			return new FakeLattice(false);
		}

		@Override
		public FakeLattice bottom() {
			return new FakeLattice(true);
		}

		@Override
		public boolean lessOrEqual(
				FakeLattice other) {
			return bottom;
		}

		@Override
		public FakeLattice lub(
				FakeLattice other) {
			return new FakeLattice(bottom && other.bottom);
		}

		@Override
		public boolean knowsIdentifier(
				Identifier id) {
			return false;
		}

		@Override
		public FakeLattice forgetIdentifier(
				Identifier id,
				ProgramPoint pp) {
			return this;
		}

		@Override
		public FakeLattice forgetIdentifiers(
				Iterable<Identifier> ids,
				ProgramPoint pp) {
			return this;
		}

		@Override
		public FakeLattice forgetIdentifiersIf(
				Predicate<Identifier> test,
				ProgramPoint pp) {
			return this;
		}

		@Override
		public FakeLattice pushScope(
				ScopeToken token,
				ProgramPoint pp) {
			return this;
		}

		@Override
		public FakeLattice popScope(
				ScopeToken token,
				ProgramPoint pp) {
			return this;
		}

		@Override
		public FakeLattice store(
				Identifier target,
				Identifier source) {
			return this;
		}

		@Override
		public StructuredRepresentation representation() {
			return new StringRepresentation(bottom ? "_|_" : "normal");
		}
	}

	private static final class FakeValueDomain
			implements
			ValueDomain<FakeLattice> {

		@Override
		public FakeLattice assign(
				FakeLattice state,
				Identifier id,
				ValueExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle) {
			return state;
		}

		@Override
		public FakeLattice smallStepSemantics(
				FakeLattice state,
				ValueExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle) {
			return state;
		}

		@Override
		public FakeLattice assume(
				FakeLattice state,
				ValueExpression expression,
				ProgramPoint src,
				ProgramPoint dest,
				SemanticOracle oracle) {
			return state;
		}

		@Override
		public FakeLattice makeLattice() {
			return new FakeLattice(false);
		}

		@Override
		public boolean canProcess(
				ValueExpression e,
				ProgramPoint pp,
				SemanticOracle oracle) {
			return true;
		}
	}

	@Test
	public void testDefaultConstraintsIsEmptyWhenStateIsNotBottom()
			throws SemanticException {
		FakeValueDomain domain = new FakeValueDomain();
		Set<BinaryExpression> constraints = domain.constraints(domain, new FakeLattice(false), x, pp(), null);
		assertTrue(constraints.isEmpty());
	}

	@Test
	public void testDefaultConstraintsIsNullWhenStateIsBottom()
			throws SemanticException {
		FakeValueDomain domain = new FakeValueDomain();
		Set<BinaryExpression> constraints = domain.constraints(domain, new FakeLattice(true), x, pp(), null);
		assertNull(constraints);
	}

}
