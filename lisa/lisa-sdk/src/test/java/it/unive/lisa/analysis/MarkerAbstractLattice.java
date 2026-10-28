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
public class MarkerAbstractLattice
		implements
		AbstractLattice<MarkerAbstractLattice>,
		BaseLattice<MarkerAbstractLattice> {

	private static final MarkerAbstractLattice TOP = new MarkerAbstractLattice(null, "top");

	private static final MarkerAbstractLattice BOTTOM = new MarkerAbstractLattice(null, "bottom");

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
	public MarkerAbstractLattice() {
		this(new HashSet<>(), "init");
	}

	/**
	 * Builds a fresh, non-top, non-bottom instance that knows exactly the given
	 * identifiers.
	 *
	 * @param known the identifiers this instance knows about
	 */
	public MarkerAbstractLattice(
			Set<Identifier> known) {
		this(known, "init");
	}

	private MarkerAbstractLattice(
			Set<Identifier> known,
			String lastOperation) {
		this.known = known;
		this.lastOperation = lastOperation;
	}

	private MarkerAbstractLattice with(
			String operation) {
		return new MarkerAbstractLattice(known == null ? null : new HashSet<>(known), operation);
	}

	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation(lastOperation);
	}

	@Override
	public MarkerAbstractLattice withTopMemory() {
		return isTop() || isBottom() ? this : with("withTopMemory");
	}

	@Override
	public MarkerAbstractLattice withTopValues() {
		return isTop() || isBottom() ? this : with("withTopValues");
	}

	@Override
	public MarkerAbstractLattice withTopTypes() {
		return isTop() || isBottom() ? this : with("withTopTypes");
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return !isTop() && !isBottom() && known.contains(id);
	}

	@Override
	public MarkerAbstractLattice forgetIdentifier(
			Identifier id,
			ProgramPoint pp) {
		if (isTop() || isBottom())
			return this;
		Set<Identifier> copy = new HashSet<>(known);
		copy.remove(id);
		return new MarkerAbstractLattice(copy, "forgetIdentifier:" + id);
	}

	@Override
	public MarkerAbstractLattice forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp) {
		if (isTop() || isBottom())
			return this;
		Set<Identifier> copy = new HashSet<>(known);
		copy.removeIf(test);
		return new MarkerAbstractLattice(copy, "forgetIdentifiersIf");
	}

	@Override
	public MarkerAbstractLattice forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp) {
		if (isTop() || isBottom())
			return this;
		Set<Identifier> copy = new HashSet<>(known);
		ids.forEach(copy::remove);
		return new MarkerAbstractLattice(copy, "forgetIdentifiers");
	}

	@Override
	public MarkerAbstractLattice pushScope(
			ScopeToken scope,
			ProgramPoint pp) {
		return isTop() || isBottom() ? this : with("push:" + scope);
	}

	@Override
	public MarkerAbstractLattice popScope(
			ScopeToken scope,
			ProgramPoint pp) {
		return isTop() || isBottom() ? this : with("pop:" + scope);
	}

	@Override
	public boolean lessOrEqualAux(
			MarkerAbstractLattice other) {
		return other.known.containsAll(known);
	}

	@Override
	public MarkerAbstractLattice lubAux(
			MarkerAbstractLattice other) {
		Set<Identifier> union = new HashSet<>(known);
		union.addAll(other.known);
		return new MarkerAbstractLattice(union, "lub");
	}

	@Override
	public MarkerAbstractLattice top() {
		return TOP;
	}

	@Override
	public MarkerAbstractLattice bottom() {
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
		if (!(obj instanceof MarkerAbstractLattice))
			return false;
		MarkerAbstractLattice other = (MarkerAbstractLattice) obj;
		if (isTop() != other.isTop() || isBottom() != other.isBottom())
			return false;
		return Objects.equals(known, other.known);
	}

	@Override
	public String toString() {
		return representation().toString();
	}

}
