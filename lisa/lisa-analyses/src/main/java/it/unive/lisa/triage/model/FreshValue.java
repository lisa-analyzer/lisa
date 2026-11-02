package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.Type;

/**
 * A value that is unknown when a transition is executed, and that is thus
 * represented by a fresh, unconstrained symbol in the transition. Each
 * evaluation of a source of unknown values produces a distinct fresh value,
 * uniquely identified by its name. Fresh values are either {@link InputValue}s,
 * that model the nondeterminism of the program and are part of its inputs, or
 * {@link SummaryValue}s, that model values that the transition does not compute
 * precisely (e.g., the results of summarized calls).<br>
 * <br>
 * Fresh values are modeled as {@link Identifier}s so that they can appear
 * inside symbolic expressions, but they are never assigned and are not affected
 * by scoping.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class FreshValue
		extends
		Identifier {

	/**
	 * Builds the fresh value.
	 *
	 * @param staticType the static type of the value
	 * @param name       the unique name of the value
	 * @param location   the location where the value is produced
	 */
	protected FreshValue(
			Type staticType,
			String name,
			CodeLocation location) {
		super(staticType, name, false, new Annotations(), location);
	}

	@Override
	public boolean canBeScoped() {
		return false;
	}

	@Override
	public SymbolicExpression pushScope(
			ScopeToken token,
			ProgramPoint pp) {
		return this;
	}

	@Override
	public SymbolicExpression popScope(
			ScopeToken token,
			ProgramPoint pp) {
		return this;
	}

	@Override
	public <T> T accept(
			ExpressionVisitor<T> visitor,
			Object... params)
			throws SemanticException {
		return visitor.visit(this, params);
	}

	@Override
	public SymbolicExpression replace(
			SymbolicExpression source,
			SymbolicExpression target) {
		if (this.equals(source))
			return target;
		return this;
	}

	@Override
	public String toString() {
		return getName();
	}
}
