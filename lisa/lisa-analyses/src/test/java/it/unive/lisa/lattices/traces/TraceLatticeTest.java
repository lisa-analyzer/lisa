package it.unive.lisa.lattices.traces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.lattices.heap.Monolith;
import it.unive.lisa.lattices.numeric.SignLattice;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.Variable;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class TraceLatticeTest {

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);

	private final Variable x = new Variable(Int32Type.INSTANCE, "x", pp.getLocation());

	private final SimpleAbstractState<Monolith,
			ValueEnvironment<SignLattice>,
			TypeEnvironment<TypeSet>> singleton = new SimpleAbstractState<>(
					Monolith.SINGLETON,
					new ValueEnvironment<>(new SignLattice()),
					new TypeEnvironment<>(new TypeSet()));

	private SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>> stateWith(
			SignLattice sign) {
		return new SimpleAbstractState<>(
				Monolith.SINGLETON,
				new ValueEnvironment<>(new SignLattice()).putState(x, sign),
				new TypeEnvironment<>(new TypeSet()));
	}

	private TraceLattice<
			SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> mk(
					Map<ExecutionTrace,
							SimpleAbstractState<Monolith,
									ValueEnvironment<SignLattice>,
									TypeEnvironment<TypeSet>>> function) {
		return new TraceLattice<>(singleton, function);
	}

	@Test
	public void topHasNoTraces() {
		assertTrue(mk(new HashMap<>()).top().isTop());
	}

	@Test
	public void bottomIsBottom() {
		assertTrue(mk(new HashMap<>()).bottom().isBottom());
	}

	@Test
	public void collapseOnTopYieldsTheUnderlyingLatticeTop() {
		TraceLattice<
				SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> top = mk(
						new HashMap<>()).top();
		assertTrue(top.collapse().isTop());
	}

	@Test
	public void collapseOnBottomYieldsTheUnderlyingLatticeBottom() {
		TraceLattice<
				SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> bottom = mk(
						new HashMap<>()).bottom();
		assertTrue(bottom.collapse().isBottom());
	}

	@Test
	public void collapseOverApproximatesAllTracesWithTheirLub()
			throws SemanticException {
		Map<ExecutionTrace,
				SimpleAbstractState<Monolith,
						ValueEnvironment<SignLattice>,
						TypeEnvironment<TypeSet>>> function = new HashMap<>();
		function.put(ExecutionTrace.EMPTY, stateWith(SignLattice.POS));
		function.put(ExecutionTrace.EMPTY.push(new Branching(pp, true)), stateWith(SignLattice.NEG));

		SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>> expected = stateWith(SignLattice.POS).lub(stateWith(SignLattice.NEG));

		assertEquals(expected, mk(function).collapse());
	}

	@Test
	public void knowsIdentifierIsTrueIfAnyTraceKnowsIt()
			throws SemanticException {
		Map<ExecutionTrace,
				SimpleAbstractState<Monolith,
						ValueEnvironment<SignLattice>,
						TypeEnvironment<TypeSet>>> function = new HashMap<>();
		function.put(ExecutionTrace.EMPTY, singleton);
		function.put(ExecutionTrace.EMPTY.push(new Branching(pp, true)), stateWith(SignLattice.POS));

		assertTrue(mk(function).knowsIdentifier(x));
	}

	@Test
	public void knowsIdentifierIsFalseWhenNoTraceKnowsIt() {
		Map<ExecutionTrace,
				SimpleAbstractState<Monolith,
						ValueEnvironment<SignLattice>,
						TypeEnvironment<TypeSet>>> function = new HashMap<>();
		function.put(ExecutionTrace.EMPTY, singleton);
		assertFalse(mk(function).knowsIdentifier(x));
	}

	@Test
	public void knowsIdentifierOnTopIsFalse() {
		// top has no concrete traces to look the identifier up in
		assertFalse(mk(new HashMap<>()).top().knowsIdentifier(x));
	}

	@Test
	public void forgetIdentifierRemovesItFromEveryTrace()
			throws SemanticException {
		Map<ExecutionTrace,
				SimpleAbstractState<Monolith,
						ValueEnvironment<SignLattice>,
						TypeEnvironment<TypeSet>>> function = new HashMap<>();
		function.put(ExecutionTrace.EMPTY, stateWith(SignLattice.POS));
		function.put(ExecutionTrace.EMPTY.push(new Branching(pp, true)), stateWith(SignLattice.NEG));

		TraceLattice<
				SimpleAbstractState<Monolith,
						ValueEnvironment<SignLattice>,
						TypeEnvironment<TypeSet>>> forgotten = mk(function).forgetIdentifier(x, pp);

		assertFalse(forgotten.knowsIdentifier(x));
	}

	@Test
	public void forgetIdentifierOnTopIsANoOp()
			throws SemanticException {
		TraceLattice<
				SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> top = mk(
						new HashMap<>()).top();
		assertTrue(top.forgetIdentifier(x, pp).isTop());
	}

	@Test
	public void withTopValuesIsAppliedToEveryTrace()
			throws SemanticException {
		Map<ExecutionTrace,
				SimpleAbstractState<Monolith,
						ValueEnvironment<SignLattice>,
						TypeEnvironment<TypeSet>>> function = new HashMap<>();
		ExecutionTrace trace = ExecutionTrace.EMPTY.push(new Branching(pp, true));
		function.put(trace, stateWith(SignLattice.POS));

		TraceLattice<
				SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> result = mk(
						function).withTopValues();

		assertEquals(stateWith(SignLattice.POS).withTopValues(), result.getState(trace));
	}

}
