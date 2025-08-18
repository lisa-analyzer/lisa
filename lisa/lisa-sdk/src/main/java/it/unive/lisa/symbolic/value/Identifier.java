package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

/**
 * An identifier of a program variable, representing either a program variable
 * (as an instance of {@link Variable}), or a resolved memory location (as an
 * instance of {@link HeapLocation}).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Identifier extends ValueExpression {

	/**
	 * The name of the identifier
	 */
	private final String name;

	/**
	 * Whether or not this identifier is weak, meaning that it should only
	 * receive weak assignments
	 */
	private final boolean weak;

	private final Annotations annotations;

	/**
	 * Builds the identifier.
	 * 
	 * @param staticType  the static type of this expression
	 * @param name        the name of the identifier
	 * @param weak        whether or not this identifier is weak, meaning that
	 *                        it should only receive weak assignments
	 * @param annotations the annotations of this identifier
	 * @param location    the code location of the statement that has generated
	 *                        this identifier
	 */
	protected Identifier(
			Type staticType,
			String name,
			boolean weak,
			Annotations annotations,
			CodeLocation location) {
		super(staticType, location);
		this.name = name;
		this.weak = weak;
		this.annotations = annotations;
	}

	/**
	 * Yields the name of this identifier.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Yields whether or not this identifier is weak. Weak identifiers should
	 * only receive weak assignments, that is, the value of the identifier after
	 * the assignment should be the least upper bound between its old value and
	 * the fresh value being assigned. With strong identifiers instead, the new
	 * value corresponds to the freshly provided value.
	 * 
	 * @return {@code true} if this identifier is weak, {@code false} otherwise
	 */
	public boolean isWeak() {
		return weak;
	}

	/**
	 * Yields {@code true} if this is an {@link OutOfScopeIdentifier} whose (i)
	 * scope was introduced by a call, or (ii) inner identifier is an
	 * {@link OutOfScopeIdentifier} such that {@code inner.isScopedByCall()}
	 * holds.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isScopedByCall() {
		return false;
	}

	/**
	 * Yields {@code true} if a call to
	 * {@link #pushScope(ScopeToken, ProgramPoint)} on this identifier yields a
	 * new {@link OutOfScopeIdentifier} associated with the given scope, that
	 * can then be removed by {@link #popScope(ScopeToken, ProgramPoint)}. If
	 * this method returns {@code false}, then
	 * {@link #pushScope(ScopeToken, ProgramPoint)} and
	 * {@link #popScope(ScopeToken, ProgramPoint)} will always return this
	 * identifier instead.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public abstract boolean canBeScoped();

	@Override
	public int hashCode() {
		final int prime = 31;
		// we do not call super here since variables should be uniquely
		// identified by their name, regardless of their type
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		// we do not call super here since variables should be uniquely
		// identified by their name, regardless of their type
		if (getClass() != obj.getClass())
			return false;
		Identifier other = (Identifier) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	/**
	 * Yields the annotations of this identifier.
	 * 
	 * @return the annotations of this identifier
	 */
	public Annotations getAnnotations() {
		return annotations;
	}

	/**
	 * Adds an annotation to the annotations of this identifier.
	 * 
	 * @param ann the annotation to be added
	 */
	public void addAnnotation(
			Annotation ann) {
		annotations.addAnnotation(ann);
	}

	/**
	 * Yields the least upper bounds between two identifiers.
	 * 
	 * @param other the other identifier
	 * 
	 * @return the least upper bounds between two identifiers.
	 * 
	 * @throws SemanticException if this and other are not equal.
	 */
	public Identifier lub(
			Identifier other)
			throws SemanticException {
		if (!equals(other))
			throw new SemanticException(
				"Cannot perform the least upper bound between different identifiers: '"
					+ this
					+ "' and '"
					+ other
					+ "'");
		return this;
	}

	@Override
	public boolean mightNeedRewriting() {
		Type t = getStaticType();
		return !t.isValueType() || t.isUntyped();
	}

	@Override
	public SymbolicExpression removeTypingExpressions() {
		return this;
	}

	/**
	 * Yields whether or not this identifier is an instrumented receiver, that
	 * is, a special variable reference that is used to represent objects,
	 * arrays, or other data structures that are being initialized and that have
	 * not been assigned to a variable yet, and thus live on the stack.
	 * 
	 * @return {@code true} if this identifier is an instrumented receiver,
	 *             {@code false} otherwise
	 */
	public boolean isInstrumentedReceiver() {
		return false;
	}

}
