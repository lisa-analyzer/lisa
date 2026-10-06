package it.unive.lisa.analysis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.lattices.SingleHeapLattice;
import it.unive.lisa.lattices.SingleTypeLattice;
import it.unive.lisa.lattices.SingleValueLattice;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

/**
 * Tests for the orchestration logic of {@link SimpleAbstractDomain}: how it
 * defaults missing sub-domains to their no-op counterparts, how it combines the
 * three sub-domains in {@link SimpleAbstractDomain#makeLattice()} and
 * {@link SimpleAbstractDomain#makeOracle(Object)}, and the reference-equality
 * optimization in
 * {@link SimpleAbstractDomain#onCallReturn(Object, Object, it.unive.lisa.program.cfg.ProgramPoint)}.
 * The substitution-application logic of {@code SimpleAbstractDomain} (its
 * private {@code applySubstitution} method) is instead covered by
 * {@link SubstitutionTest}, together with the rest of the identifier
 * substitution machinery it relies on.
 */
public class SimpleAbstractDomainTest {

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

	/**
	 * A {@link NoOpTypes} whose {@code onCallReturn} unconditionally answers
	 * {@link SingleTypeLattice#BOTTOM}, regardless of its arguments. Used to
	 * force the type component of the result of
	 * {@link SimpleAbstractDomain#onCallReturn} to differ from the one of
	 * {@code callres}.
	 */
	private static class ResettingTypeDomain
			extends
			NoOpTypes {

		@Override
		public SingleTypeLattice onCallReturn(
				SingleTypeLattice entryState,
				SingleTypeLattice callres,
				ProgramPoint call)
				throws SemanticException {
			return SingleTypeLattice.BOTTOM;
		}

	}

	@Test
	public void testHeapOnlyConstructorDefaultsValueAndTypeToNoOp() {
		SimpleAbstractDomain<SingleHeapLattice,
				SingleValueLattice,
				SingleTypeLattice> domain = new SimpleAbstractDomain<>(
						new NoOpHeap());
		assertTrue(domain.heapDomain instanceof NoOpHeap);
		assertTrue(domain.valueDomain instanceof NoOpValues);
		assertTrue(domain.typeDomain instanceof NoOpTypes);
	}

	@Test
	public void testValueOnlyConstructorDefaultsHeapAndTypeToNoOp() {
		SimpleAbstractDomain<SingleHeapLattice,
				SingleValueLattice,
				SingleTypeLattice> domain = new SimpleAbstractDomain<>(
						new NoOpValues());
		assertTrue(domain.heapDomain instanceof NoOpHeap);
		assertTrue(domain.valueDomain instanceof NoOpValues);
		assertTrue(domain.typeDomain instanceof NoOpTypes);
	}

	@Test
	public void testTypeOnlyConstructorDefaultsHeapAndValueToNoOp() {
		SimpleAbstractDomain<SingleHeapLattice,
				SingleValueLattice,
				SingleTypeLattice> domain = new SimpleAbstractDomain<>(
						new NoOpTypes());
		assertTrue(domain.heapDomain instanceof NoOpHeap);
		assertTrue(domain.valueDomain instanceof NoOpValues);
		assertTrue(domain.typeDomain instanceof NoOpTypes);
	}

	@Test
	public void testHeapAndValueConstructorDefaultsTypeToNoOp() {
		SimpleAbstractDomain<SingleHeapLattice,
				SingleValueLattice,
				SingleTypeLattice> domain = new SimpleAbstractDomain<>(
						new NoOpHeap(),
						new NoOpValues());
		assertTrue(domain.typeDomain instanceof NoOpTypes);
	}

	@Test
	public void testHeapAndTypeConstructorDefaultsValueToNoOp() {
		SimpleAbstractDomain<SingleHeapLattice,
				SingleValueLattice,
				SingleTypeLattice> domain = new SimpleAbstractDomain<>(
						new NoOpHeap(),
						new NoOpTypes());
		assertTrue(domain.valueDomain instanceof NoOpValues);
	}

	@Test
	public void testValueAndTypeConstructorDefaultsHeapToNoOp() {
		SimpleAbstractDomain<SingleHeapLattice,
				SingleValueLattice,
				SingleTypeLattice> domain = new SimpleAbstractDomain<>(
						new NoOpValues(),
						new NoOpTypes());
		assertTrue(domain.heapDomain instanceof NoOpHeap);
	}

	@Test
	public void testMakeLatticeCombinesTheThreeSubDomains() {
		SimpleAbstractDomain<SingleHeapLattice,
				SingleValueLattice,
				SingleTypeLattice> domain = new SimpleAbstractDomain<>(
						new NoOpHeap(),
						new NoOpValues(),
						new NoOpTypes());

		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> state = domain.makeLattice();

		assertSame(SingleHeapLattice.SINGLETON, state.heapState);
		assertSame(SingleValueLattice.SINGLETON, state.valueState);
		assertSame(SingleTypeLattice.SINGLETON, state.typeState);
	}

	@Test
	public void testOnCallReturnReusesCallresWhenNoSubDomainChangesIt()
			throws SemanticException {
		SimpleAbstractDomain<SingleHeapLattice,
				SingleValueLattice,
				SingleTypeLattice> domain = new SimpleAbstractDomain<>(
						new NoOpHeap(),
						new NoOpValues(),
						new NoOpTypes());
		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> entry = domain.makeLattice();
		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> callres = domain.makeLattice();

		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> result = domain.onCallReturn(
				entry,
				callres,
				fake);

		// none of the (no-op) sub-domains changes anything, so the very same
		// callres instance should be returned, avoiding an unnecessary
		// allocation
		assertSame(callres, result);
	}

	@Test
	public void testOnCallReturnBuildsANewStateWhenASubDomainChangesIt()
			throws SemanticException {
		SimpleAbstractDomain<SingleHeapLattice,
				SingleValueLattice,
				SingleTypeLattice> domain = new SimpleAbstractDomain<>(
						new NoOpHeap(),
						new NoOpValues(),
						new ResettingTypeDomain());
		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> entry = domain.makeLattice();
		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> callres = domain.makeLattice();

		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> result = domain.onCallReturn(
				entry,
				callres,
				fake);

		// the type sub-domain always answers BOTTOM regardless of its inputs,
		// so the type component of the result differs from callres' one, and a
		// brand-new state combining the (possibly) updated components must be
		// built rather than blindly returning callres
		assertNotSame(callres, result);
		assertSame(SingleTypeLattice.BOTTOM, result.typeState);
		assertSame(callres.heapState, result.heapState);
		assertSame(callres.valueState, result.valueState);
	}

	@Test
	public void testMakeOracleDelegatesToTheSubDomains()
			throws SemanticException {
		SimpleAbstractDomain<SingleHeapLattice,
				SingleValueLattice,
				SingleTypeLattice> domain = new SimpleAbstractDomain<>(
						new NoOpHeap(),
						new NoOpValues(),
						new NoOpTypes());
		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> state = domain.makeLattice();

		SemanticOracle oracle = domain.makeOracle(state);

		// x does not need rewriting, so the oracle short-circuits without even
		// consulting the (no-op) heap domain
		assertTrue(oracle.rewrite(x, fake).elements().contains(x));
		assertSame(Satisfiability.UNKNOWN, oracle.alias(x, x, fake));
		assertSame(Satisfiability.UNKNOWN, oracle.isReachableFrom(x, x, fake));
		assertSame(Untyped.INSTANCE, oracle.getDynamicTypeOf(x, fake));
		// NoOpValues is not a WholeValueAnalysis participant
		assertFalse(oracle.hasWholeValueAnlysis());
		assertTrue(oracle.constraints(new NoOpValues(), x, fake).isEmpty());
		// no event queue was ever set on the domain
		assertSame(null, oracle.getEventQueue());
	}

	@Test
	public void testMakeOracleRewriteDelegatesToHeapDomainWhenRewritingIsNeeded()
			throws SemanticException {
		SimpleAbstractDomain<SingleHeapLattice,
				SingleValueLattice,
				SingleTypeLattice> domain = new SimpleAbstractDomain<>(
						new NoOpHeap(),
						new NoOpValues(),
						new NoOpTypes());
		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> state = domain.makeLattice();
		SemanticOracle oracle = domain.makeOracle(state);

		ExpressionSet result = oracle.rewrite(new ExpressionSet(x), fake);
		assertTrue(result.elements().contains(x));
	}

}
