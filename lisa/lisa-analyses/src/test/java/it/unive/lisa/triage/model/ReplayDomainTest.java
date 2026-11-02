package it.unive.lisa.triage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.interprocedural.UniqueScope;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.lattices.heap.Monolith;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMul;
import it.unive.lisa.triage.inputs.InputSource;
import it.unive.lisa.triage.inputs.InputSourceResolver;
import it.unive.lisa.util.numeric.IntInterval;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReplayDomainTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("test", 1, 1);

	private static final Variable X = new Variable(Int32Type.INSTANCE, "x", LOC);

	private static final Variable Y = new Variable(Int32Type.INSTANCE, "y", LOC);

	private static final Variable Z = new Variable(Int32Type.INSTANCE, "z", LOC);

	private static final UniqueScope<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>,
			TypeEnvironment<TypeSet>>> CTX = new UniqueScope<>();

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);

	private FreshValueFactory factory;

	private ReplayDomain<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
			SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> domain;

	private Analysis<ReplayState<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>,
			TypeEnvironment<TypeSet>>>,
			ReplayDomain<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
					SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>>> analysis;

	@BeforeEach
	public void setup() {
		factory = new FreshValueFactory(new InputSourceResolver(List.of(), null));
		domain = new ReplayDomain<>(DefaultConfiguration.defaultAbstractDomain(), factory, 2);
		analysis = new Analysis<>(domain);
	}

	private AnalysisState<ReplayState<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>,
			TypeEnvironment<TypeSet>>>> start() {
		ProgramState<ReplayState<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>,
				TypeEnvironment<TypeSet>>>> execution = new ProgramState<>(
						domain.lift(DefaultConfiguration.defaultAbstractDomain().makeLattice().top()),
						new ExpressionSet());
		return new AnalysisState<>(execution).bottom().withExecution(execution);
	}

	private static Constant integer(
			int value) {
		return new Constant(Int32Type.INSTANCE, value, LOC);
	}

	private static BinaryExpression plus(
			SymbolicExpression l,
			SymbolicExpression r) {
		return new BinaryExpression(Int32Type.INSTANCE, l, r, NumericNonOverflowingAdd.INSTANCE, LOC);
	}

	private static BinaryExpression times(
			SymbolicExpression l,
			SymbolicExpression r) {
		return new BinaryExpression(Int32Type.INSTANCE, l, r, NumericNonOverflowingMul.INSTANCE, LOC);
	}

	private static BinaryExpression gt(
			SymbolicExpression l,
			SymbolicExpression r) {
		return new BinaryExpression(BoolType.INSTANCE, l, r, ComparisonGt.INSTANCE, LOC);
	}

	private static SymbolicCase single(
			AnalysisState<? extends ReplayState<?>> state) {
		Set<SymbolicCase> cases = state.getExecutionState().getStore().getCases();
		assertEquals(1, cases.size(), "Expected a single case: " + cases);
		return cases.iterator().next();
	}

	@Test
	public void assignmentsAreExpressedOverInitialValues() throws SemanticException {
		var state = start();
		state = analysis.assign(state, X, plus(Y, integer(1)), CTX, pp);
		state = analysis.assign(state, Z, times(X, integer(2)), CTX, pp);
		SymbolicCase c = single(state);
		assertEquals(plus(Y, integer(1)), c.getUpdates().get(X));
		assertEquals(times(plus(Y, integer(1)), integer(2)), c.getUpdates().get(Z));
		assertFalse(c.isUnconstrained(Y));
		// the abstract state is updated as during the analysis
		state = analysis.assign(state, Y, integer(5), CTX, pp);
		assertEquals(new IntInterval(5, 5),
				state.getExecutionState().getInner().valueState.getState(Y));
	}

	@Test
	public void assumptionsGoToThePathCondition() throws SemanticException {
		var state = start();
		state = analysis.assign(state, X, plus(Y, integer(1)), CTX, pp);
		state = analysis.assume(state, gt(X, integer(0)), CTX, pp, pp);
		assertEquals(List.of(gt(plus(Y, integer(1)), integer(0))), single(state).getPathCondition());
	}

	@Test
	public void unknownValuesBecomeFreshValues() throws SemanticException {
		var state = start();
		state = analysis.assign(state, X, new PushAny(Int32Type.INSTANCE, LOC), CTX, pp);
		state = analysis.assign(state, Y, new SummaryMarker(Int32Type.INSTANCE, "call result", LOC), CTX, pp);
		SymbolicCase c = single(state);
		InputValue input = (InputValue) c.getUpdates().get(X);
		assertEquals(InputSource.Kind.UNKNOWN, input.getSource().getKind());
		SummaryValue summary = (SummaryValue) c.getUpdates().get(Y);
		assertEquals("call result", summary.getReason());
		assertEquals(Map.of(input.getName(), input, summary.getName(), summary), factory.getValues());
	}

	@Test
	public void heapIsHavocked() throws SemanticException {
		HeapLocation cell = new HeapLocation(Int32Type.INSTANCE, "cell", false, LOC);
		var state = start();
		state = analysis.assign(state, cell, integer(1), CTX, pp);
		SymbolicCase c = single(state);
		assertTrue(c.isHeapHavoc());
		assertTrue(c.isUnconstrained(cell));
		// reading the heap yields a summary
		state = analysis.assign(state, X, cell, CTX, pp);
		assertTrue(single(state).getUpdates().get(X) instanceof SummaryValue);
	}

	@Test
	public void forgottenIdentifiersAreUnconstrained() throws SemanticException {
		var state = start();
		state = analysis.assign(state, X, integer(1), CTX, pp);
		state = state.forgetIdentifiers(List.<Identifier>of(X), pp);
		SymbolicCase c = single(state);
		assertTrue(c.isUnconstrained(X));
		state = analysis.assign(state, Y, X, CTX, pp);
		assertTrue(single(state).getUpdates().get(Y) instanceof SummaryValue);
	}

	@Test
	public void joinsKeepAlternativesUpToTheBound() throws SemanticException {
		var base = start();
		var one = analysis.assign(base, X, integer(1), CTX, pp);
		var two = analysis.assign(base, X, integer(2), CTX, pp);
		var joined = one.lub(two);
		assertEquals(2, joined.getExecutionState().getStore().getCases().size());

		// the bound is 2: a third alternative merges all cases
		var three = analysis.assume(analysis.assign(base, X, integer(3), CTX, pp), gt(Y, integer(0)), CTX, pp, pp);
		var merged = joined.lub(three);
		SymbolicCase c = single(merged);
		assertTrue(c.getUpdates().get(X) instanceof SummaryValue);
		assertTrue(c.getPathCondition().isEmpty());
	}

	@Test
	public void latticeShortcutsDoNotLoseCases() throws SemanticException {
		var base = start();
		var one = analysis.assign(base, X, integer(1), CTX, pp);
		// same abstract state as one, different store
		var other = analysis.assume(one, gt(Y, Y), CTX, pp, pp);
		assertTrue(one.lessOrEqual(one.lub(other)));
		assertTrue(other.lessOrEqual(one.lub(other)));
		assertFalse(one.lessOrEqual(other));
		assertEquals(2, one.lub(other).getExecutionState().getStore().getCases().size());
	}

	@Test
	public void innerDomainsAreVisible() {
		// the components of the wrapped domain can be retrieved through the
		// wrapper
		ValueDomain<?> value = domain.getDomainInstance(ValueDomain.class);
		assertTrue(value instanceof Interval, String.valueOf(value));
		assertTrue(domain.getDomainInstance(ReplayDomain.class) == domain);
		assertTrue(domain.getDomainInstance(SimpleAbstractDomain.class) != null);
		assertTrue(domain.getDomainInstance(String.class) == null);
	}
}
