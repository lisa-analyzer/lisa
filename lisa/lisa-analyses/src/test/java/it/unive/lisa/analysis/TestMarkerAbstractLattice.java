package it.unive.lisa.analysis;

import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A minimal, hand-rolled {@link AbstractLattice} used by tests in this package
 * to observe, without resorting to a mocking framework, how classes under test
 * (e.g. {@link ProgramState}, {@link AnalysisState}, {@link Analysis}) delegate
 * identifier- and scope-related operations to the wrapped abstract state.
 * <p>
 * Unlike {@code it.unive.lisa.TestAbstractState} (whose {@code top()} and
 * {@code bottom()} both return {@code this}, making every instance
 * simultaneously top and bottom), this class has a real, distinguishable top
 * and bottom, a set of identifiers it "knows about" that participates in a
 * proper (subset-based) lattice ordering, and every produced instance records,
 * in {@link #lastOperation}, a description of the call that produced it, so
 * that tests can assert on which operation was actually invoked.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TestMarkerAbstractLattice
		implements
		AbstractLattice<TestMarkerAbstractLattice>,
		BaseLattice<TestMarkerAbstractLattice> {

	private static final TestMarkerAbstractLattice TOP = new TestMarkerAbstractLattice(null, "top");

	private static final TestMarkerAbstractLattice BOTTOM = new TestMarkerAbstractLattice(null, "bottom");

	/**
	 * The identifiers this (fake) state currently knows about. This is
	 * {@code null} for {@link #TOP} and {@link #BOTTOM}, as their identifier
	 * set is not meaningful.
	 */
	public final Set<Identifier> known;

	/**
	 * A textual description of the last operation that produced this instance,
	 * useful for tests to assert on which delegate method has been invoked.
	 */
	public final String lastOperation;

	/**
	 * Builds a fresh, non-top, non-bottom instance that knows no identifiers.
	 */
	public TestMarkerAbstractLattice() {
		this(new HashSet<>(), "init");
	}

	/**
	 * Builds a fresh, non-top, non-bottom instance that knows exactly the given
	 * identifiers.
	 *
	 * @param known the identifiers this instance knows about
	 */
	public TestMarkerAbstractLattice(
			Set<Identifier> known) {
		this(known, "init");
	}

	private TestMarkerAbstractLattice(
			Set<Identifier> known,
			String lastOperation) {
		this.known = known;
		this.lastOperation = lastOperation;
	}

	private TestMarkerAbstractLattice with(
			String operation) {
		return new TestMarkerAbstractLattice(known == null ? null : new HashSet<>(known), operation);
	}

	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation(lastOperation);
	}

	@Override
	public TestMarkerAbstractLattice withTopMemory() {
		return isTop() || isBottom() ? this : with("withTopMemory");
	}

	@Override
	public TestMarkerAbstractLattice withTopValues() {
		return isTop() || isBottom() ? this : with("withTopValues");
	}

	@Override
	public TestMarkerAbstractLattice withTopTypes() {
		return isTop() || isBottom() ? this : with("withTopTypes");
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return !isTop() && !isBottom() && known.contains(id);
	}

	@Override
	public TestMarkerAbstractLattice forgetIdentifier(
			Identifier id,
			ProgramPoint pp) {
		if (isTop() || isBottom())
			return this;
		Set<Identifier> copy = new HashSet<>(known);
		copy.remove(id);
		return new TestMarkerAbstractLattice(copy, "forgetIdentifier:" + id);
	}

	@Override
	public TestMarkerAbstractLattice forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp) {
		if (isTop() || isBottom())
			return this;
		Set<Identifier> copy = new HashSet<>(known);
		copy.removeIf(test);
		return new TestMarkerAbstractLattice(copy, "forgetIdentifiersIf");
	}

	@Override
	public TestMarkerAbstractLattice forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp) {
		if (isTop() || isBottom())
			return this;
		Set<Identifier> copy = new HashSet<>(known);
		ids.forEach(copy::remove);
		return new TestMarkerAbstractLattice(copy, "forgetIdentifiers");
	}

	@Override
	public TestMarkerAbstractLattice pushScope(
			ScopeToken scope,
			ProgramPoint pp) {
		return isTop() || isBottom() ? this : with("push:" + scope);
	}

	@Override
	public TestMarkerAbstractLattice popScope(
			ScopeToken scope,
			ProgramPoint pp) {
		return isTop() || isBottom() ? this : with("pop:" + scope);
	}

	@Override
	public boolean lessOrEqualAux(
			TestMarkerAbstractLattice other) {
		return other.known.containsAll(known);
	}

	@Override
	public TestMarkerAbstractLattice lubAux(
			TestMarkerAbstractLattice other) {
		Set<Identifier> union = new HashSet<>(known);
		union.addAll(other.known);
		return new TestMarkerAbstractLattice(union, "lub");
	}

	@Override
	public TestMarkerAbstractLattice top() {
		return TOP;
	}

	@Override
	public TestMarkerAbstractLattice bottom() {
		return BOTTOM;
	}

	@Override
	public int hashCode() {
		return Objects.hash(known, isTop(), isBottom());
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof TestMarkerAbstractLattice))
			return false;
		TestMarkerAbstractLattice other = (TestMarkerAbstractLattice) obj;
		if (isTop() != other.isTop() || isBottom() != other.isBottom())
			return false;
		return Objects.equals(known, other.known);
	}

	@Override
	public String toString() {
		return representation().toString();
	}

}
