package it.unive.lisa.symbolic;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.OutOfScopeIdentifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Objects;
import java.util.Set;

/**
 * A symbolic expression that can be evaluated by {@link SemanticDomain}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class SymbolicExpression {

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
	 * The runtime types of this expression
	 */
	private Set<Type> types;

	/**
	 * Builds the symbolic expression.
	 * 
	 * @param staticType the static type of this expression
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	protected SymbolicExpression(Type staticType, CodeLocation location) {
		Objects.requireNonNull(staticType, "The static type of a symbolic expression cannot be null");
		Objects.requireNonNull(location, "The location of a symbolic expression cannot be null");
		this.staticType = staticType;
		this.location = location;
	}

	/**
	 * Yields the static type of this expression.
	 * 
	 * @return the static type
	 */
	public Type getStaticType() {
		return staticType;
	}

	/**
	 * Yields the runtime types of this expression. If
	 * {@link #setRuntimeTypes(Set)} has never been called before, this method
	 * will return all instances of the static type.
	 * 
	 * @param types the type system that knows about the types of the program
	 *                  point where this method is called. If
	 *                  {@link #hasRuntimeTypes()} returns {@code true}, this
	 *                  parameter can be {@code null}
	 * 
	 * @return the runtime types
	 */
	public final Set<Type> getRuntimeTypes(TypeSystem types) {
		if (this.types != null)
			return this.types;
		if (types == null)
			throw new IllegalArgumentException("Cannot use a null type system if no runtime types are available");
		return staticType.allInstances(types);
	}

	/**
	 * Sets the runtime types to the given set of types.
	 * 
	 * @param types the runtime types
	 */
	public void setRuntimeTypes(Set<Type> types) {
		this.types = types;
	}

	/**
	 * Yields {@code true} if this expression's runtime types have been set
	 * (even to the empty set). If this method returns {@code false}, then
	 * {@link #getDynamicType()} will yield the same as
	 * {@link #getStaticType()}, and {@link #getRuntimeTypes(TypeSystem)}
	 * returns all possible instances of the static type.
	 * 
	 * @return whether or not runtime types are set for this expression
	 */
	public boolean hasRuntimeTypes() {
		return types != null;
	}

	/**
	 * Yields the dynamic type of this expression, that is, the most specific
	 * common supertype of all its runtime types (available through
	 * {@link #getRuntimeTypes(TypeSystem)}. If {@link #setRuntimeTypes(Set)}
	 * has never been called before, this method will return the static type.
	 * 
	 * @return the dynamic type of this expression
	 */
	public final Type getDynamicType() {
		return Type.commonSupertype(types, staticType);
	}

	/**
	 * Pushes a new scope, identified by the give token, in the expression. This
	 * causes all {@link Variable}s to become {@link OutOfScopeIdentifier}s
	 * associated with the given token.
	 *
	 * @param token the token identifying the scope to push
	 * 
	 * @return a copy of this expression where the local variables have gone out
	 *             of scope
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public abstract SymbolicExpression pushScope(ScopeToken token) throws SemanticException;

	/**
	 * Pops the scope identified by the given token from the expression. This
	 * causes all the invisible variables (i.e. {@link OutOfScopeIdentifier}s)
	 * mapped to the given scope to become visible (i.e. {@link Variable}s)
	 * again.
	 *
	 * @param token the token of the scope to be restored
	 * 
	 * @return a copy of this expression where the local variables associated
	 *             with the given scope are visible again
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public abstract SymbolicExpression popScope(ScopeToken token) throws SemanticException;

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
	public abstract <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((staticType == null) ? 0 : staticType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
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
}
