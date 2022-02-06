package it.unive.lisa.analysis.value;

import static it.unive.lisa.util.collections.CollectionUtilities.collect;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapSemanticOperation.HeapReplacement;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.common.Int32;
import it.unive.lisa.util.collections.CollectionsDiffBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;

public class SubstitutionTest {

	private static class Collector implements ValueDomain<Collector> {

		private final ExpressionSet<Identifier> assigned, removed;

		private Collector() {
			this.assigned = new ExpressionSet<>(new HashSet<>());
			this.removed = new ExpressionSet<>(new HashSet<>());
		}

		private Collector(Collector other) {
			this.assigned = new ExpressionSet<>(other.assigned.elements());
			this.removed = new ExpressionSet<>(other.removed.elements());
		}

		@Override
		public Collector assign(Identifier id, ValueExpression expression, ProgramPoint pp) throws SemanticException {
			Collector add = new Collector(this);
			add.assigned.elements().add(id);
			return add;
		}

		@Override
		public Collector smallStepSemantics(ValueExpression expression, ProgramPoint pp) throws SemanticException {
			return null; // not used
		}

		@Override
		public Collector assume(ValueExpression expression, ProgramPoint pp) throws SemanticException {
			return null; // not used
		}

		@Override
		public Collector forgetIdentifier(Identifier id) throws SemanticException {
			Collector rem = new Collector(this);
			rem.removed.elements().add(id);
			return rem;
		}

		@Override
		public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp) throws SemanticException {
			return null; // not used
		}

		@Override
		public Collector pushScope(ScopeToken token) throws SemanticException {
			return null; // not used
		}

		@Override
		public Collector popScope(ScopeToken token) throws SemanticException {
			return null; // not used
		}

		@Override
		public DomainRepresentation representation() {
			return null; // not used
		}

		@Override
		public Collector lub(Collector other) throws SemanticException {
			Collector lub = new Collector(this);
			lub.assigned.elements().addAll(other.assigned.elements());
			lub.removed.elements().addAll(other.removed.elements());
			return lub;
		}

		@Override
		public Collector widening(Collector other) throws SemanticException {
			return null; // not used
		}

		@Override
		public boolean lessOrEqual(Collector other) throws SemanticException {
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

	private final Variable x = new Variable(Int32.INSTANCE, "x", SyntheticLocation.INSTANCE);
	private final Variable y = new Variable(Int32.INSTANCE, "y", SyntheticLocation.INSTANCE);
	private final Variable z = new Variable(Int32.INSTANCE, "z", SyntheticLocation.INSTANCE);
	private final Variable w = new Variable(Int32.INSTANCE, "w", SyntheticLocation.INSTANCE);
	private final Comparator<Identifier> comparer = (l, r) -> l.getName().compareTo(r.getName());

	private void check(List<HeapReplacement> sub,
			Collection<Identifier> addexpected,
			Collection<Identifier> remexpected)
			throws SemanticException {
		Collector c = new Collector().applySubstitution(sub, fake);

		CollectionsDiffBuilder<
				Identifier> add = new CollectionsDiffBuilder<>(Identifier.class, addexpected, c.assigned.elements());
		CollectionsDiffBuilder<
				Identifier> rem = new CollectionsDiffBuilder<>(Identifier.class, remexpected, c.removed.elements());
		add.compute(comparer);
		rem.compute(comparer);

		assertTrue("Applying " + sub + " assigned unexpected identifiers: " + add.getOnlySecond(),
				add.getOnlySecond().isEmpty());
		assertTrue("Applying " + sub + " removed unexpected identifiers: " + rem.getOnlySecond(),
				rem.getOnlySecond().isEmpty());
		assertTrue("Applying " + sub + " did not assign some identifiers: " + add.getOnlyFirst(),
				add.getOnlyFirst().isEmpty());
		assertTrue("Applying " + sub + " did not remove some identifiers: " + rem.getOnlyFirst(),
				rem.getOnlyFirst().isEmpty());
	}

	@Test
	public void testEmptySubstitution() throws SemanticException {
		check(null, collect(), collect());
		check(new ArrayList<>(), collect(), collect());
		check(Arrays.asList(new HeapReplacement()), collect(), collect());
	}

	@Test
	public void testSingleSubstitution() throws SemanticException {
		HeapReplacement rep = new HeapReplacement();
		rep.addSource(x);
		rep.addTarget(y);

		check(Arrays.asList(rep), collect(y), collect(x));
	}

	@Test
	public void testSingleWeakSubstitution() throws SemanticException {
		HeapReplacement rep = new HeapReplacement();
		rep.addSource(x);
		rep.addTarget(x);
		rep.addTarget(y);

		check(Arrays.asList(rep), collect(y, x), collect());
	}

	@Test
	public void testNonInterferingSubstitution() throws SemanticException {
		HeapReplacement rep1 = new HeapReplacement();
		rep1.addSource(x);
		rep1.addTarget(y);
		HeapReplacement rep2 = new HeapReplacement();
		rep2.addSource(z);
		rep2.addTarget(w);

		check(Arrays.asList(rep1, rep2), collect(y, w), collect(x, z));
	}

	@Test
	public void testInterferingSubstitution() throws SemanticException {
		HeapReplacement rep1 = new HeapReplacement();
		rep1.addSource(x);
		rep1.addTarget(y);
		HeapReplacement rep2 = new HeapReplacement();
		rep2.addSource(w);
		rep2.addTarget(x);

		check(Arrays.asList(rep1, rep2), collect(y, x), collect(x, w));
	}

	@Test
	public void testResettingSubstitution() throws SemanticException {
		HeapReplacement rep1 = new HeapReplacement();
		rep1.addSource(x);
		rep1.addTarget(y);
		HeapReplacement rep2 = new HeapReplacement();
		HeapReplacement rep3 = new HeapReplacement();
		rep3.addSource(z);
		rep3.addTarget(w);

		check(Arrays.asList(rep1, rep2, rep3), collect(y, w), collect(x, z));
	}
}
