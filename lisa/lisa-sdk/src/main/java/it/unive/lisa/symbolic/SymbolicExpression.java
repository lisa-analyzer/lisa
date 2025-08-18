package it.unive.lisa.symbolic;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.ScopedObject;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.OutOfScopeIdentifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import java.util.Objects;

/**
 * A symbolic expression that can be evaluated by {@link SemanticDomain}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class SymbolicExpression implements ScopedObject<SymbolicExpression> {

	/**
	 * The code location of the statement that has generated this symbolic
	 * expression. The code location is not used for the equality between two
	 * symbolic expressions.
	 */
	private final CodeLocation location;

	/**
	 * The static type of this expression
	 */
	private final Type staticType;

	/**
	 * Builds the symbolic expression.
	 * 
	 * @param staticType the static type of this expression, determined at its
	 *                       construction
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	protected SymbolicExpression(
			Type staticType,
			CodeLocation location) {
		Objects.requireNonNull(staticType, "The static type of a symbolic expression cannot be null");
		Objects.requireNonNull(location, "The location of a symbolic expression cannot be null");
		this.staticType = staticType;
		this.location = location;
	}

	/**
	 * Yields the static type of this expression, as provided during
	 * construction. The returned type should (but might not) be a supertype of
	 * all the runtime types determined during the analysis, with the exception
	 * of {@link HeapLocation}s representing more memory locations.
	 * 
	 * @return the static type
	 */
	public Type getStaticType() {
		return staticType;
	}

	/**
	 * Pushes a new scope, identified by the give token, in the expression. This
	 * causes all {@link Identifier}s where {@link Identifier#canBeScoped()}
	 * holds to become {@link OutOfScopeIdentifier}s associated with the given
	 * token.
	 *
	 * @param token the token identifying the scope to push
	 * 
	 * @return a copy of this expression where the local variables have gone out
	 *             of scope
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@Override
	public abstract SymbolicExpression pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException;

	/**
	 * Pops the scope identified by the given token from the expression. This
	 * causes all the invisible variables (i.e. {@link OutOfScopeIdentifier}s)
	 * mapped to the given scope to become visible (e.g. {@link Variable}s)
	 * again. Note that invoking this method on {@link Identifier}s where
	 * {@link Identifier#canBeScoped()} holds will return {@code null} if (i)
	 * the identifier is not scoped, or (ii) the identifier is scoped by a
	 * different token.
	 *
	 * @param token the token of the scope to be restored
	 * 
	 * @return a copy of this expression where the local variables associated
	 *             with the given scope are visible again
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@Override
	public abstract SymbolicExpression popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException;

	/**
	 * Accepts an {@link ExpressionVisitor}, visiting this expression
	 * recursively.
	 * 
	 * @param <T>     the type of value produced by the visiting callbacks
	 * @param visitor the visitor
	 * @param params  additional optional parameters to pass to each visiting
	 *                    callback
	 * 
	 * @return the value produced by the visiting operation
	 * 
	 * @throws SemanticException if an error occurs during the visiting
	 */
	public abstract <T> T accept(
			ExpressionVisitor<T> visitor,
			Object... params)
			throws SemanticException;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((staticType == null) ? 0 : staticType.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SymbolicExpression other = (SymbolicExpression) obj;
		if (staticType == null) {
			if (other.staticType != null)
				return false;
		} else if (!staticType.equals(other.staticType))
			return false;
		return true;
	}

	/**
	 * Yields the code location of the statement that has generated this
	 * symbolic expression. The code location is not used for the equality
	 * between two symbolic expressions.
	 * 
	 * @return the code location of the statement that has generated this
	 *             symbolic expression
	 */
	public CodeLocation getCodeLocation() {
		return location;
	}

	@Override
	public abstract String toString();

	/**
	 * Yields whether or not this expression can be considered safe to be
	 * processed by domains that only focus on {@link ValueExpression}s, or if
	 * it might need rewriting. This is a definite answer: if this method
	 * returns {@code true}, than this expression is sure to not contain
	 * anything that deals with, or points to, the memory.
	 * 
	 * @return whether or not this expression might need rewriting
	 */
	public abstract boolean mightNeedRewriting();

	/**
	 * Extracts the inner expressions from casts/conversions. If {@code this} is
	 * of the form {@code e cast/conv type}, this method returns
	 * {@code removeTypingExpressions(e)}. Otherwise, {@code this} is returned
	 * after invoking this method on all sub-expressions.
	 * 
	 * @return the typing expression-free version of {@code this}
	 */
	public abstract SymbolicExpression removeTypingExpressions();

	/**
	 * Replaces all occurrences of the given source expression with the target
	 * expression in this expression. If the source is not present, this
	 * expression is returned as-is.
	 * 
	 * @param source the expression to be replaced
	 * @param target the expression to replace with
	 * 
	 * @return the updated expression
	 */
	public abstract SymbolicExpression replace(
			SymbolicExpression source,
			SymbolicExpression target);

}
