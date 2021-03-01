package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.program.cfg.statement.Call;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.VoidType;

/**
 * An expression that does nothing.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Skip extends ValueExpression {

	/**
	 * Builds the skip.
	 */
	public Skip() {
		super(Caches.types().mkSingletonSet(VoidType.INSTANCE));
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ getClass().getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "skip";
	}


	@Override
	public SymbolicExpression pushScope(Call scope) {
		return this;
	}

	@Override
	public SymbolicExpression popScope(Call scope) throws SemanticException {
		return this;
	}
}
