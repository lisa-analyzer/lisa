package it.unive.lisa.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.lattices.HistoryState;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.lattices.heap.Monolith;
import it.unive.lisa.lattices.numeric.SignLattice;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import org.junit.jupiter.api.Test;

public class HistoryDomainTest {

	private final SimpleAbstractDomain<Monolith,
			ValueEnvironment<SignLattice>,
			TypeEnvironment<TypeSet>> inner = new SimpleAbstractDomain<>(new MonolithicHeap(), new Sign(),
					new InferredTypes());

	private final HistoryDomain<
			SimpleAbstractDomain<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>,
			SimpleAbstractState<Monolith,
					ValueEnvironment<SignLattice>,
					TypeEnvironment<TypeSet>>> domain = new HistoryDomain<>(inner);

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);

	private final Identifier x = new Variable(Int32Type.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private final Constant five = new Constant(Int32Type.INSTANCE, 5, SyntheticLocation.INSTANCE);

	private HistoryState<
			SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> initial() {
		return new HistoryState<>(inner.makeLattice());
	}

	@Test
	public void makeLatticeWrapsTheUnderlyingInitialState() {
		assertEquals(inner.makeLattice(), domain.makeLattice().head());
	}

	@Test
	public void assignDelegatesAndWrapsTheResult()
			throws SemanticException {
		HistoryState<
				SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> result = domain
						.assign(initial(), x, five, pp);
		assertEquals(inner.assign(inner.makeLattice(), x, five, pp), result.head());
	}

	@Test
	public void assignDiscardsAnyPreexistingHistory()
			throws SemanticException {
		HistoryState<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> withHistory = initial().upchainAux(initial());
		HistoryState<
				SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> result = domain
						.assign(withHistory, x, five, pp);

		int count = 0;
		for (@SuppressWarnings("unused")
		SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>> s : result)
			count++;
		assertEquals(1, count);
	}

	@Test
	public void smallStepSemanticsDelegatesAndWrapsTheResult()
			throws SemanticException {
		HistoryState<
				SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> result = domain
						.smallStepSemantics(initial(), five, pp);
		assertEquals(inner.smallStepSemantics(inner.makeLattice(), five, pp), result.head());
	}

	@Test
	public void assumeDelegatesAndWrapsTheResult()
			throws SemanticException {
		HistoryState<
				SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> result = domain
						.assume(initial(), five, pp, pp);
		assertEquals(inner.assume(inner.makeLattice(), five, pp, pp), result.head());
	}

	@Test
	public void satisfiesDelegatesDirectlyToTheUnderlyingDomain()
			throws SemanticException {
		assertEquals(inner.satisfies(inner.makeLattice(), five, pp), domain.satisfies(initial(), five, pp));
	}

	@Test
	public void onCallReturnDelegatesAndWrapsTheResult()
			throws SemanticException {
		HistoryState<
				SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> result = domain
						.onCallReturn(initial(), initial(), pp);
		assertEquals(inner.onCallReturn(inner.makeLattice(), inner.makeLattice(), pp), result.head());
	}

	@Test
	public void makeOracleDelegatesToTheUnderlyingDomain() {
		assertTrue(domain.makeOracle(initial()) != null);
	}

}
