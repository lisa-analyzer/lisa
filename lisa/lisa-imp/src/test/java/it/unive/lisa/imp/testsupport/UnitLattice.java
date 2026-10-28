package it.unive.lisa.imp.testsupport;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

// a degenerate AbstractLattice tracking no information at all: it exists
// purely so that Analysis<UnitLattice, RecordingDomain> can be instantiated
// to drive a Statement's semantics methods without depending on a real
// concrete domain from lisa-analyses (which would create a module cycle)
public final class UnitLattice
		implements
		AbstractLattice<UnitLattice>,
		BaseLattice<UnitLattice> {

	public static final UnitLattice INSTANCE = new UnitLattice(false, false);

	public static final UnitLattice TOP = new UnitLattice(true, false);

	public static final UnitLattice BOTTOM = new UnitLattice(false, true);

	private final boolean top;

	private final boolean bottom;

	private UnitLattice(
			boolean top,
			boolean bottom) {
		this.top = top;
		this.bottom = bottom;
	}

	@Override
	public boolean isTop() {
		return top;
	}

	@Override
	public boolean isBottom() {
		return bottom;
	}

	@Override
	public UnitLattice top() {
		return TOP;
	}

	@Override
	public UnitLattice bottom() {
		return BOTTOM;
	}

	@Override
	public UnitLattice lubAux(
			UnitLattice other)
			throws SemanticException {
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(
			UnitLattice other)
			throws SemanticException {
		return true;
	}

	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation(bottom ? "_|_" : top ? "#TOP#" : "unit");
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return false;
	}

	@Override
	public UnitLattice forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public UnitLattice forgetIdentifiersIf(
			java.util.function.Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public UnitLattice forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public UnitLattice pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public UnitLattice popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public UnitLattice withTopMemory() {
		return this;
	}

	@Override
	public UnitLattice withTopValues() {
		return this;
	}

	@Override
	public UnitLattice withTopTypes() {
		return this;
	}

	@Override
	public String toString() {
		return representation().toString();
	}

}
