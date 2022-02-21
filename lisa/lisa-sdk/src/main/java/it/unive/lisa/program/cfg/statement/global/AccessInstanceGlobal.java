package it.unive.lisa.program.cfg.statement.global;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * An access to an instance {@link Global} of a {@link CompilationUnit}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AccessInstanceGlobal extends UnaryExpression {

	/**
	 * The global being accessed
	 */
	private final Global target;

	/**
	 * Builds the global access, happening at the given location in the program.
	 * The type of this expression is the one of the accessed global.
	 * 
	 * @param cfg      the cfg that this expression belongs to
	 * @param location the location where the expression is defined within the
	 *                     program
	 * @param receiver the expression that determines the accessed instance
	 * @param target   the accessed global
	 */
	public AccessInstanceGlobal(CFG cfg, CodeLocation location, Expression receiver, Global target) {
		super(cfg, location, "::", target.getStaticType(), receiver);
		this.target = target;
		receiver.setParentStatement(this);
	}

	/**
	 * Yields the expression that determines the receiver of the global access
	 * defined by this expression.
	 * 
	 * @return the receiver of the access
	 */
	public Expression getReceiver() {
		return getSubExpression();
	}

	/**
	 * Yields the instance {@link Global} targeted by this expression.
	 * 
	 * @return the global
	 */
	public Global getTarget() {
		return target;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AccessInstanceGlobal other = (AccessInstanceGlobal) obj;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getSubExpression() + "::" + target.getName();
	}

	@Override
	protected <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> unarySemantics(
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					AnalysisState<A, H, V, T> state,
					SymbolicExpression expr,
					StatementStore<A, H, V, T> expressions)
					throws SemanticException {
		Variable var = new Variable(
				target.getStaticType(),
				target.getName(),
				target.getAnnotations(),
				target.getLocation());

		ExternalSet<Type> rectypes = Caches.types().mkEmptySet();
		for (Type t : expr.getRuntimeTypes())
			if (t.isPointerType())
				rectypes.addAll(t.asPointerType().getInnerTypes());

		if (rectypes.isEmpty())
			return state.bottom();

		Type rectype = rectypes.reduce(rectypes.first(), (r, t) -> r.commonSupertype(t));
		HeapDereference container = new HeapDereference(rectype, expr, getLocation());
		container.setRuntimeTypes(rectypes);
		AccessChild access = new AccessChild(var.getStaticType(), container, var, getLocation());
		return state.smallStepSemantics(access, this);
	}
}
