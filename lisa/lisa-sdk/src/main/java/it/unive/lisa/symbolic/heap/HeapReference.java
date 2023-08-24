package it.unive.lisa.symbolic.heap;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

/**
 * A reference to a memory location, identified by its name.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class HeapReference extends HeapExpression {

	/**
	 * The expression referred by this expression
	 */
	private final SymbolicExpression expression;

	/**
	 * Builds the heap reference.
	 * 
	 * @param staticType the static type of this expression
	 * @param expression the expression that this refers to
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public HeapReference(Type staticType, SymbolicExpression expression, CodeLocation location) {
		super(staticType, location);
		this.expression = expression;
	}

	@Override
	public String toString() {
		return "ref$" + expression.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
		return result;
	}

	/**
	 * Yields the referred expression.
	 * 
	 * @return the referred expression
	 */
	public SymbolicExpression getExpression() {
		return expression;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		HeapReference other = (HeapReference) obj;
		if (expression == null) {
			if (other.expression != null)
				return false;
		} else if (!expression.equals(other.expression))
			return false;
		return true;
	}

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		T l = expression.accept(visitor, params);
		return visitor.visit(this, l, params);
	}
	
	@Override
	public SymbolicExpression pushScope(ScopeToken token) {
		try {
			return new HeapReference(getStaticType(), expression.pushScope(token), getCodeLocation());
		} catch (SemanticException e) {
			// TODO: this is here to make the code compile, change this ASAP
			throw new IllegalStateException();
		}
	}
	
	@Override
	public SymbolicExpression popScope(ScopeToken token) throws SemanticException {
		try {
			return new HeapReference(getStaticType(), expression.popScope(token), getCodeLocation());
		} catch (SemanticException e) {
			// TODO: this is here to make the code compile, change this ASAP
			throw new IllegalStateException();
		} 
	}
}
