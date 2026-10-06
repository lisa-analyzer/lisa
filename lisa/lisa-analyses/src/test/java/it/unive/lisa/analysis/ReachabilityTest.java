package it.unive.lisa.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.lattices.ReachLattice;
import it.unive.lisa.lattices.ReachLattice.ReachabilityStatus;
import it.unive.lisa.lattices.ReachabilityProduct;
import it.unive.lisa.lattices.Satisfiability;
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

public class ReachabilityTest {

	private final SimpleAbstractDomain<Monolith,
			ValueEnvironment<SignLattice>,
			TypeEnvironment<TypeSet>> inner = new SimpleAbstractDomain<>(new MonolithicHeap(), new Sign(),
					new InferredTypes());

	private final Reachability<
			SimpleAbstractDomain<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>,
			SimpleAbstractState<Monolith,
					ValueEnvironment<SignLattice>,
					TypeEnvironment<TypeSet>>> domain = new Reachability<>(inner);

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);

	private final Identifier x = new Variable(Int32Type.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private final Constant five = new Constant(Int32Type.INSTANCE, 5, SyntheticLocation.INSTANCE);

	private ReachabilityProduct<
			SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> reachable() {
		return new ReachabilityProduct<>(new ReachLattice().setToReachable(), inner.makeLattice());
	}

	private ReachabilityProduct<
			SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> unreachable() {
		return new ReachabilityProduct<>(new ReachLattice(ReachabilityStatus.UNREACHABLE, null), inner.makeLattice());
	}

	@Test
	public void makeLatticeStartsAsReachable() {
		assertEquals(ReachabilityStatus.REACHABLE, domain.makeLattice().first.lattice);
		assertEquals(inner.makeLattice(), domain.makeLattice().second);
	}

	@Test
	public void assignAtANonStatementPointJustDelegates()
			throws SemanticException {
		ReachabilityProduct<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> state = reachable();
		ReachabilityProduct<
				SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> result = domain
						.assign(state, x, five, pp);
		assertEquals(inner.assign(state.second, x, five, pp), result.second);
		assertEquals(state.first, result.first);
	}

	@Test
	public void smallStepSemanticsAtANonStatementPointJustDelegates()
			throws SemanticException {
		ReachabilityProduct<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> state = reachable();
		ReachabilityProduct<
				SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> result = domain
						.smallStepSemantics(state, five, pp);
		assertEquals(inner.smallStepSemantics(state.second, five, pp), result.second);
	}

	@Test
	public void assumeAtANonStatementPointJustDelegates()
			throws SemanticException {
		ReachabilityProduct<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> state = reachable();
		ReachabilityProduct<
				SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> result = domain
						.assume(state, five, pp, pp);
		assertEquals(inner.assume(state.second, five, pp, pp), result.second);
		assertEquals(state.first, result.first);
	}

	@Test
	public void satisfiesIsBottomWhenTheStateIsUnreachable()
			throws SemanticException {
		assertEquals(Satisfiability.BOTTOM, domain.satisfies(unreachable(), five, pp));
	}

	@Test
	public void satisfiesDelegatesWhenTheStateIsReachable()
			throws SemanticException {
		ReachabilityProduct<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> state = reachable();
		assertEquals(inner.satisfies(state.second, five, pp), domain.satisfies(state, five, pp));
	}

	@Test
	public void onCallReturnRestoresReachabilityIfTheCallItselfDidNotEscape()
			throws SemanticException {
		ReachabilityProduct<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> entry = reachable();
		ReachabilityProduct<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> callres = unreachable();
		ReachabilityProduct<
				SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> result = domain
						.onCallReturn(entry, callres, pp);
		assertEquals(ReachabilityStatus.REACHABLE, result.first.lattice);
	}

	@Test
	public void onCallReturnKeepsCallResultReachabilityWhenEntryWasNotReachable()
			throws SemanticException {
		ReachabilityProduct<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> entry = unreachable();
		ReachabilityProduct<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> callres = reachable();
		ReachabilityProduct<
				SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> result = domain
						.onCallReturn(entry, callres, pp);
		assertEquals(ReachabilityStatus.REACHABLE, result.first.lattice);
	}

	@Test
	public void makeOracleDelegatesToTheUnderlyingDomain() {
		assertTrue(domain.makeOracle(reachable()) != null);
	}

}
