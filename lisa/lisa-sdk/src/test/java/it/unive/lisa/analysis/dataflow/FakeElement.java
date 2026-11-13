package it.unive.lisa.analysis.dataflow;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A minimal {@link DataflowElement} used as a test fixture: it just tracks a
 * label and a set of involved identifiers, and knows how to rename occurrences
 * of one identifier into another.
 */
final class FakeElement
		implements
		DataflowElement<FakeElement> {

	private final String label;

	private final Set<Identifier> involved;

	FakeElement(
			String label,
			Identifier... involved) {
		this.label = label;
		this.involved = new HashSet<>(Set.of(involved));
	}

	private FakeElement(
			String label,
			Set<Identifier> involved) {
		this.label = label;
		this.involved = involved;
	}

	@Override
	public Collection<Identifier> getInvolvedIdentifiers() {
		return involved;
	}

	@Override
	public FakeElement replaceIdentifier(
			Identifier source,
			Identifier target) {
		if (!involved.contains(source))
			return this;
		Set<Identifier> updated = new HashSet<>(involved);
		updated.remove(source);
		updated.add(target);
		return new FakeElement(label + "'", updated);
	}

	@Override
	public FakeElement pushScope(
			ScopeToken token,
			ProgramPoint pp) {
		return this;
	}

	@Override
	public FakeElement popScope(
			ScopeToken token,
			ProgramPoint pp) {
		return this;
	}

	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation(label);
	}

	@Override
	public String toString() {
		return label;
	}

	@Override
	public int hashCode() {
		return Objects.hash(label, involved);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof FakeElement))
			return false;
		FakeElement other = (FakeElement) obj;
		return label.equals(other.label) && involved.equals(other.involved);
	}

}
