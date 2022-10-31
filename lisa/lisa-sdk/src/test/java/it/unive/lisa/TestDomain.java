package it.unive.lisa;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public abstract class TestDomain<T extends TestDomain<T, E>, E extends SymbolicExpression> extends BaseLattice<T>
		implements SemanticDomain<T, E, Identifier> {

	@Override
	public T top() {
		return (T) this;
	}

	@Override
	public T bottom() {
		return (T) this;
	}

	@Override
	public T assign(Identifier id, E expression, ProgramPoint pp) throws SemanticException {
		return (T) this;
	}

	@Override
	public T smallStepSemantics(E expression, ProgramPoint pp) throws SemanticException {
		return (T) this;
	}

	@Override
	public T assume(E expression, ProgramPoint pp) throws SemanticException {
		return (T) this;
	}

	@Override
	public T forgetIdentifier(Identifier id) throws SemanticException {
		return (T) this;
	}

	@Override
	public T forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
		return (T) this;
	}

	@Override
	public Satisfiability satisfies(E expression, ProgramPoint pp) throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public T pushScope(ScopeToken token) throws SemanticException {
		return (T) this;
	}

	@Override
	public T popScope(ScopeToken token) throws SemanticException {
		return (T) this;
	}

	@Override
	public T lubAux(T other) throws SemanticException {
		return (T) this;
	}

	@Override
	public boolean lessOrEqualAux(T other) throws SemanticException {
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public String toString() {
		return representation().toString();
	}
}
