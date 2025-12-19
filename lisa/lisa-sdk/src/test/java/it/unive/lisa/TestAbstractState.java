package it.unive.lisa;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.function.Predicate;

public class TestAbstractState
		implements
		AbstractLattice<TestAbstractState> {

	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation("state");
	}

	@Override
	public TestAbstractState withTopMemory() {
		return this;
	}

	@Override
	public TestAbstractState withTopValues() {
		return this;
	}

	@Override
	public TestAbstractState withTopTypes() {
		return this;
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return false;
	}

	@Override
	public TestAbstractState forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public TestAbstractState forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public TestAbstractState forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public boolean lessOrEqual(
			TestAbstractState other)
			throws SemanticException {
		return true;
	}

	@Override
	public TestAbstractState lub(
			TestAbstractState other)
			throws SemanticException {
		return this;
	}

	@Override
	public TestAbstractState merge(
			TestAbstractState other)
			throws SemanticException {
		return this;
	}

	@Override
	public TestAbstractState top() {
		return this;
	}

	@Override
	public TestAbstractState bottom() {
		return this;
	}

	@Override
	public TestAbstractState pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public TestAbstractState popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

}
