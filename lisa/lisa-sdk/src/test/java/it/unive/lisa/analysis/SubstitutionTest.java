package it.unive.lisa.analysis;

import static it.unive.lisa.util.collections.CollectionUtilities.collect;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.analysis.heap.HeapLattice;
import it.unive.lisa.analysis.type.TypeDomain;
import it.unive.lisa.analysis.type.TypeLattice;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.collections.CollectionsDiffBuilder;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public class SubstitutionTest {

	/**
	 * A {@link ValueLattice} and {@link TypeLattice} that just records, in two
	 * separate sets, which identifiers have been stored ({@link #assigned}) and
	 * which have been forgotten ({@link #removed}). It is deliberately never
	 * top nor bottom (see {@link #top()}/{@link #bottom()}) so that
	 * {@link #applyReplacement(HeapReplacement, ProgramPoint)} always exercises
	 * its full logic instead of short-circuiting. The very same class is used
	 * both as the value and as the type lattice of a
	 * {@link SimpleAbstractDomain} in
	 * {@link SubstitutionTest#testSimpleAbstractDomainAppliesSubstitutionsToTypeAndValueInLockstep()}
	 * and
	 * {@link SubstitutionTest#testSimpleAbstractDomainAppliesSubstitutionsAcrossRewrittenAlternatives()},
	 * so that a single substitution sequence can be checked to have been
	 * applied identically to both.
	 */
	private static class Collector
			implements
			ValueLattice<Collector>,
			TypeLattice<Collector> {

		private final ExpressionSet assigned, removed;

		private Collector() {
			this.assigned = new ExpressionSet(new HashSet<>());
			this.removed = new ExpressionSet(new HashSet<>());
		}

		private Collector(
				Collector other) {
			this.assigned = new ExpressionSet(other.assigned.elements());
			this.removed = new ExpressionSet(other.removed.elements());
		}

		@Override
		public Collector forgetIdentifier(
				Identifier id,
				ProgramPoint pp)
				throws SemanticException {
			Collector rem = new Collector(this);
			rem.removed.elements().add(id);
			return rem;
		}

		@Override
		public Collector forgetIdentifiers(
				Iterable<Identifier> ids,
				ProgramPoint pp)
				throws SemanticException {
			Collector rem = new Collector(this);
			ids.forEach(rem.removed.elements()::add);
			return rem;
		}

		@Override
		public Collector forgetIdentifiersIf(
				Predicate<Identifier> test,
				ProgramPoint pp)
				throws SemanticException {
			return null;
		}

		@Override
		public Collector pushScope(
				ScopeToken token,
				ProgramPoint pp)
				throws SemanticException {
			return null; // not used
		}

		@Override
		public Collector popScope(
				ScopeToken token,
				ProgramPoint pp)
				throws SemanticException {
			return null; // not used
		}

		@Override
		public StructuredRepresentation representation() {
			return null; // not used
		}

		@Override
		public Collector lub(
				Collector other)
				throws SemanticException {
			Collector lub = new Collector(this);
			lub.assigned.elements().addAll(other.assigned.elements());
			lub.removed.elements().addAll(other.removed.elements());
			return lub;
		}

		@Override
		public Collector upchain(
				Collector other)
				throws SemanticException {
			return lub(other);
		}

		@Override
		public Collector downchain(
				Collector other)
				throws SemanticException {
			return glb(other);
		}

		@Override
		public boolean lessOrEqual(
				Collector other)
				throws SemanticException {
			return false; // not used
		}

		@Override
		public Collector top() {
			return null; // not used
		}

		@Override
		public Collector bottom() {
			return new Collector();
		}

		@Override
		public boolean knowsIdentifier(
				Identifier id) {
			return false; // not used
		}

		@Override
		public Collector store(
				Identifier target,
				Identifier source)
				throws SemanticException {
			Collector add = new Collector(this);
			add.assigned.elements().add(target);
			return add;
		}

	}

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

	private final Variable y = new Variable(Untyped.INSTANCE, "y", SyntheticLocation.INSTANCE);

	private final Variable z = new Variable(Untyped.INSTANCE, "z", SyntheticLocation.INSTANCE);

	private final Variable w = new Variable(Untyped.INSTANCE, "w", SyntheticLocation.INSTANCE);

	private final Comparator<SymbolicExpression> comparer = (
			l,
			r) -> ((Identifier) l).getName().compareTo(((Identifier) r).getName());

	private void check(
			List<HeapReplacement> sub,
			Collection<SymbolicExpression> addexpected,
			Collection<SymbolicExpression> remexpected)
			throws SemanticException {
		Collector c = new Collector();
		if (sub != null)
			for (HeapReplacement repl : sub)
				c = c.lub(c.applyReplacement(repl, fake));

		assertContents(c, sub, addexpected, remexpected);
	}

	/**
	 * Asserts that a {@link Collector} recorded exactly the given assigned and
	 * removed identifiers, regardless of how it was produced. This is shared
	 * between {@link #check(List, Collection, Collection)} (which builds the
	 * {@link Collector} by directly folding {@link Collector#applyReplacement}
	 * over a substitution) and the {@link SimpleAbstractDomain} integration
	 * tests below (which instead observe the {@link Collector}s produced as a
	 * side effect of
	 * {@link SimpleAbstractDomain#assign(Object, Identifier, SymbolicExpression, ProgramPoint)}/
	 * {@link SimpleAbstractDomain#smallStepSemantics(Object, SymbolicExpression, ProgramPoint)}).
	 */
	private void assertContents(
			Collector c,
			Object sub,
			Collection<SymbolicExpression> addexpected,
			Collection<SymbolicExpression> remexpected) {
		CollectionsDiffBuilder<SymbolicExpression> add = new CollectionsDiffBuilder<>(
				SymbolicExpression.class,
				addexpected,
				c.assigned.elements());
		CollectionsDiffBuilder<SymbolicExpression> rem = new CollectionsDiffBuilder<>(
				SymbolicExpression.class,
				remexpected,
				c.removed.elements());
		add.compute(comparer);
		rem.compute(comparer);

		assertTrue(
				add.getOnlySecond().isEmpty(),
				"Applying " + sub + " assigned unexpected identifiers: " + add.getOnlySecond());
		assertTrue(
				rem.getOnlySecond().isEmpty(),
				"Applying " + sub + " removed unexpected identifiers: " + rem.getOnlySecond());
		assertTrue(
				add.getOnlyFirst().isEmpty(),
				"Applying " + sub + " did not assign some identifiers: " + add.getOnlyFirst());
		assertTrue(
				rem.getOnlyFirst().isEmpty(),
				"Applying " + sub + " did not remove some identifiers: " + rem.getOnlyFirst());
	}

	@Test
	public void testEmptySubstitution()
			throws SemanticException {
		check(null, collect(), collect());
		check(new ArrayList<>(), collect(), collect());
		check(Arrays.asList(new HeapReplacement()), collect(), collect());
	}

	@Test
	public void testSingleSubstitution()
			throws SemanticException {
		HeapReplacement rep = new HeapReplacement();
		rep.addSource(x);
		rep.addTarget(y);

		check(Arrays.asList(rep), collect(y), collect(x));
	}

	@Test
	public void testSingleWeakSubstitution()
			throws SemanticException {
		HeapReplacement rep = new HeapReplacement();
		rep.addSource(x);
		rep.addTarget(x);
		rep.addTarget(y);

		check(Arrays.asList(rep), collect(y, x), collect());
	}

	@Test
	public void testNonInterferingSubstitution()
			throws SemanticException {
		HeapReplacement rep1 = new HeapReplacement();
		rep1.addSource(x);
		rep1.addTarget(y);
		HeapReplacement rep2 = new HeapReplacement();
		rep2.addSource(z);
		rep2.addTarget(w);

		check(Arrays.asList(rep1, rep2), collect(y, w), collect(x, z));
	}

	@Test
	public void testInterferingSubstitution()
			throws SemanticException {
		HeapReplacement rep1 = new HeapReplacement();
		rep1.addSource(x);
		rep1.addTarget(y);
		HeapReplacement rep2 = new HeapReplacement();
		rep2.addSource(w);
		rep2.addTarget(x);

		check(Arrays.asList(rep1, rep2), collect(y, x), collect(x, w));
	}

	@Test
	public void testResettingSubstitution()
			throws SemanticException {
		HeapReplacement rep1 = new HeapReplacement();
		rep1.addSource(x);
		rep1.addTarget(y);
		HeapReplacement rep2 = new HeapReplacement();
		HeapReplacement rep3 = new HeapReplacement();
		rep3.addSource(z);
		rep3.addTarget(w);

		check(Arrays.asList(rep1, rep2, rep3), collect(y, w), collect(x, z));
	}

	/**
	 * A trivial, non-informative {@link HeapLattice} used solely to let a
	 * {@link SimpleAbstractDomain} be assembled around a
	 * {@link TrackingHeapDomain} for the tests below: this test suite is not
	 * concerned with heap abstraction itself, only with how
	 * {@link SimpleAbstractDomain} propagates the {@link HeapReplacement}s it
	 * receives to the value and type domains.
	 */
	private static class TrackingHeapLattice
			implements
			HeapLattice<TrackingHeapLattice> {

		private static final TrackingHeapLattice INSTANCE = new TrackingHeapLattice();

		@Override
		public boolean lessOrEqual(
				TrackingHeapLattice other)
				throws SemanticException {
			return true;
		}

		@Override
		public TrackingHeapLattice lub(
				TrackingHeapLattice other)
				throws SemanticException {
			return this;
		}

		@Override
		public TrackingHeapLattice top() {
			return INSTANCE;
		}

		@Override
		public TrackingHeapLattice bottom() {
			return INSTANCE;
		}

		@Override
		public StructuredRepresentation representation() {
			return null; // not used
		}

		@Override
		public Pair<TrackingHeapLattice, List<HeapReplacement>> pushScope(
				ScopeToken token,
				ProgramPoint pp)
				throws SemanticException {
			return Pair.of(this, Collections.emptyList());
		}

		@Override
		public Pair<TrackingHeapLattice, List<HeapReplacement>> popScope(
				ScopeToken token,
				ProgramPoint pp)
				throws SemanticException {
			return Pair.of(this, Collections.emptyList());
		}

		@Override
		public boolean knowsIdentifier(
				Identifier id) {
			return false;
		}

		@Override
		public Pair<TrackingHeapLattice, List<HeapReplacement>> forgetIdentifier(
				Identifier id,
				ProgramPoint pp)
				throws SemanticException {
			return Pair.of(this, Collections.emptyList());
		}

		@Override
		public Pair<TrackingHeapLattice, List<HeapReplacement>> forgetIdentifiers(
				Iterable<Identifier> ids,
				ProgramPoint pp)
				throws SemanticException {
			return Pair.of(this, Collections.emptyList());
		}

		@Override
		public Pair<TrackingHeapLattice, List<HeapReplacement>> forgetIdentifiersIf(
				Predicate<Identifier> test,
				ProgramPoint pp)
				throws SemanticException {
			return Pair.of(this, Collections.emptyList());
		}

		@Override
		public List<HeapReplacement> expand(
				HeapReplacement base)
				throws SemanticException {
			return List.of(base);
		}

	}

	/**
	 * A {@link HeapDomain} whose {@code assign}/{@code smallStepSemantics}
	 * always yield a pre-configured list of {@link HeapReplacement}s, and whose
	 * {@code rewrite} always yields a pre-configured {@link ExpressionSet}, so
	 * that tests can drive {@link SimpleAbstractDomain} through its
	 * rewriting/substitution logic deterministically.
	 */
	private static class TrackingHeapDomain
			implements
			HeapDomain<TrackingHeapLattice> {

		private List<HeapReplacement> replacements = Collections.emptyList();

		private ExpressionSet rewritten = new ExpressionSet();

		private void configure(
				List<HeapReplacement> replacements,
				ExpressionSet rewritten) {
			this.replacements = replacements;
			this.rewritten = rewritten;
		}

		@Override
		public TrackingHeapLattice makeLattice() {
			return TrackingHeapLattice.INSTANCE;
		}

		@Override
		public Pair<TrackingHeapLattice, List<HeapReplacement>> assign(
				TrackingHeapLattice state,
				Identifier id,
				SymbolicExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return Pair.of(state, replacements);
		}

		@Override
		public Pair<TrackingHeapLattice, List<HeapReplacement>> smallStepSemantics(
				TrackingHeapLattice state,
				SymbolicExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return Pair.of(state, replacements);
		}

		@Override
		public Pair<TrackingHeapLattice, List<HeapReplacement>> assume(
				TrackingHeapLattice state,
				SymbolicExpression expression,
				ProgramPoint src,
				ProgramPoint dest,
				SemanticOracle oracle)
				throws SemanticException {
			return Pair.of(state, Collections.emptyList());
		}

		@Override
		public ExpressionSet rewrite(
				TrackingHeapLattice state,
				SymbolicExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return rewritten;
		}

		@Override
		public Satisfiability alias(
				TrackingHeapLattice state,
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return Satisfiability.UNKNOWN;
		}

		@Override
		public Satisfiability isReachableFrom(
				TrackingHeapLattice state,
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return Satisfiability.UNKNOWN;
		}

	}

	/**
	 * A {@link ValueDomain} whose transformers are the identity function,
	 * regardless of the expression being processed. Used so that the
	 * {@link Collector} observed at the end of a {@link SimpleAbstractDomain}
	 * operation reflects <b>only</b> the substitution applied by
	 * {@code SimpleAbstractDomain}, without any further modification.
	 */
	private static class IdentityValueDomain
			implements
			ValueDomain<Collector> {

		@Override
		public Collector assign(
				Collector state,
				Identifier id,
				ValueExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return state;
		}

		@Override
		public Collector smallStepSemantics(
				Collector state,
				ValueExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return state;
		}

		@Override
		public Collector assume(
				Collector state,
				ValueExpression expression,
				ProgramPoint src,
				ProgramPoint dest,
				SemanticOracle oracle)
				throws SemanticException {
			return state;
		}

		@Override
		public boolean canProcess(
				ValueExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle) {
			return true;
		}

		@Override
		public Collector makeLattice() {
			return new Collector();
		}

	}

	/**
	 * A {@link TypeDomain} whose transformers are the identity function,
	 * regardless of the expression being processed. See
	 * {@link IdentityValueDomain} for the rationale.
	 */
	private static class IdentityTypeDomain
			implements
			TypeDomain<Collector> {

		@Override
		public Collector assign(
				Collector state,
				Identifier id,
				ValueExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return state;
		}

		@Override
		public Collector smallStepSemantics(
				Collector state,
				ValueExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return state;
		}

		@Override
		public Collector assume(
				Collector state,
				ValueExpression expression,
				ProgramPoint src,
				ProgramPoint dest,
				SemanticOracle oracle)
				throws SemanticException {
			return state;
		}

		@Override
		public Set<Type> getRuntimeTypesOf(
				Collector state,
				SymbolicExpression e,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return Set.of(Untyped.INSTANCE);
		}

		@Override
		public Type getDynamicTypeOf(
				Collector state,
				SymbolicExpression e,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return Untyped.INSTANCE;
		}

		@Override
		public Collector makeLattice() {
			return new Collector();
		}

	}

	private SimpleAbstractDomain<TrackingHeapLattice, Collector, Collector> mkDomain(
			TrackingHeapDomain heapDomain) {
		return new SimpleAbstractDomain<>(heapDomain, new IdentityValueDomain(), new IdentityTypeDomain());
	}

	/**
	 * Integration test for {@link SimpleAbstractDomain}'s private
	 * {@code applySubstitution} method (invoked from
	 * {@link SimpleAbstractDomain#assign(Object, Identifier, SymbolicExpression, ProgramPoint)}
	 * whenever the assigned expression needs heap rewriting): it must apply the
	 * exact same substitution sequence, in the exact same order, to both the
	 * type and the value domain. This exercises the single-rewritten-expression
	 * path (where {@code heapDomain.rewrite} yields exactly one
	 * {@link ValueExpression}).
	 */
	@Test
	public void testSimpleAbstractDomainAppliesSubstitutionsToTypeAndValueInLockstep()
			throws SemanticException {
		HeapReplacement rep1 = new HeapReplacement();
		rep1.addSource(x);
		rep1.addTarget(y);
		HeapReplacement rep2 = new HeapReplacement();
		rep2.addSource(w);
		rep2.addTarget(x);
		List<HeapReplacement> subs = Arrays.asList(rep1, rep2);

		TrackingHeapDomain heapDomain = new TrackingHeapDomain();
		heapDomain.configure(subs, new ExpressionSet(z));

		SimpleAbstractDomain<TrackingHeapLattice, Collector, Collector> domain = mkDomain(heapDomain);
		SimpleAbstractState<TrackingHeapLattice, Collector, Collector> initial = domain.makeLattice();

		MemoryAllocation alloc = new MemoryAllocation(Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		SimpleAbstractState<TrackingHeapLattice, Collector, Collector> result = domain.assign(
				initial,
				z,
				alloc,
				fake);

		// same expectation as testInterferingSubstitution, but now observed as
		// a side effect of SimpleAbstractDomain#assign rather than by directly
		// folding applyReplacement
		assertContents(result.valueState, subs, collect(y, x), collect(x, w));
		assertContents(result.typeState, subs, collect(y, x), collect(x, w));
	}

	/**
	 * Same as
	 * {@link #testSimpleAbstractDomainAppliesSubstitutionsToTypeAndValueInLockstep()},
	 * but exercises the fan-out path taken when {@code heapDomain.rewrite}
	 * yields more than one alternative {@link ValueExpression} (as happens,
	 * e.g., when a materialization/summarization splits a single symbolic
	 * expression into several concrete ones): each alternative is evaluated
	 * from the very same post-substitution baseline and the results are lubbed
	 * together, so the type and value domains must still end up reflecting
	 * exactly the applied substitution. This uses
	 * {@link SimpleAbstractDomain#smallStepSemantics(Object, SymbolicExpression, ProgramPoint)}
	 * instead of {@code assign} to also cover that transformer.
	 */
	@Test
	public void testSimpleAbstractDomainAppliesSubstitutionsAcrossRewrittenAlternatives()
			throws SemanticException {
		HeapReplacement rep1 = new HeapReplacement();
		rep1.addSource(x);
		rep1.addTarget(y);
		HeapReplacement rep2 = new HeapReplacement();
		HeapReplacement rep3 = new HeapReplacement();
		rep3.addSource(z);
		rep3.addTarget(w);
		List<HeapReplacement> subs = Arrays.asList(rep1, rep2, rep3);

		TrackingHeapDomain heapDomain = new TrackingHeapDomain();
		heapDomain.configure(subs, new ExpressionSet(new HashSet<>(Arrays.asList(y, w))));

		SimpleAbstractDomain<TrackingHeapLattice, Collector, Collector> domain = mkDomain(heapDomain);
		SimpleAbstractState<TrackingHeapLattice, Collector, Collector> initial = domain.makeLattice();

		MemoryAllocation alloc = new MemoryAllocation(Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		SimpleAbstractState<TrackingHeapLattice, Collector, Collector> result = domain.smallStepSemantics(
				initial,
				alloc,
				fake);

		// same expectation as testResettingSubstitution
		assertContents(result.valueState, subs, collect(y, w), collect(x, z));
		assertContents(result.typeState, subs, collect(y, w), collect(x, z));
	}

}
