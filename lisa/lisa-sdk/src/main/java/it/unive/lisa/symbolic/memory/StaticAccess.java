package it.unive.lisa.symbolic.memory;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import java.util.Collection;

/**
 * A static field access ({@code p.f}), where the field name is known at compile
 * time.
 *
 * @author <a href="mailto:giacomo.boldini@unive.it">Giacomo Boldini</a>
 */
public class StaticAccess
		extends
		AccessChild<Variable> {

	private final Annotations annotations;

	/**
	 * Builds the static access.
	 *
	 * @param staticType the static type of this expression
	 * @param container  the expression representing the parent
	 * @param child      the variable representing the (constant) field being
	 *                       accessed
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public StaticAccess(
			Type staticType,
			SymbolicExpression container,
			Variable child,
			CodeLocation location) {
		super(staticType, container, child, location);
		this.annotations = new Annotations();
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
	 * Yields the list of annotations of this identifier.
	 *
	 * @return the list of annotations of this identifier
	 */
	public Collection<Annotation> getAnnotationList() {
		return annotations.getAnnotations();
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

	// TODO memory framework notation is "container.field"; kept as "->" for
	// compatibility with old tests
	@Override
	public String toString() {
		return getContainer() + "->" + getChild();
	}

	@Override
	public <T> T accept(
			ExpressionVisitor<T> visitor,
			Object... params)
			throws SemanticException {
		T cont = getContainer().accept(visitor, params);
		return visitor.visit(this, cont, getChild(), params);
	}

	@Override
	public SymbolicExpression removeTypingExpressions() {
		SymbolicExpression cont = getContainer().removeTypingExpressions();
		if (cont == getContainer())
			return this;
		return create(getStaticType(), cont, getChild(), getCodeLocation());
	}

	@Override
	public SymbolicExpression replace(
			SymbolicExpression source,
			SymbolicExpression target) {
		if (this.equals(source))
			return target;

		SymbolicExpression cont = getContainer().replace(source, target);
		if (cont == getContainer())
			return this;
		return create(getStaticType(), cont, getChild(), getCodeLocation());
	}

	@Override
	protected StaticAccess create(
			Type staticType,
			SymbolicExpression container,
			Variable child,
			CodeLocation location) {
		StaticAccess result = new StaticAccess(staticType, container, child, location);
		// Annotations don't affect equals/hashCode (inherited from
		// AccessChild), but are still preserved
		for (Annotation ann : annotations.getAnnotations())
			result.addAnnotation(ann);
		return result;
	}

}
