package it.unive.lisa.program.cfg.statement.global;

import java.util.HashSet;
import java.util.Set;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.program.language.hierarchytraversal.HierarcyTraversalStrategy;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * An access to an instance {@link Global} of a {@link ClassUnit}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AccessInstanceGlobal extends UnaryExpression {

	/**
	 * The global being accessed
	 */
	private final String target;

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
	public AccessInstanceGlobal(
			CFG cfg,
			CodeLocation location,
			Expression receiver,
			String target) {
		super(cfg, location, "::", Untyped.INSTANCE, receiver);
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
	public String getTarget() {
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
	public boolean equals(
			Object obj) {
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
		return getSubExpression() + "::" + target;
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> unarySemantics(
			InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {
		CodeLocation loc = getLocation();

		AnalysisState<A> result = state.bottom();
		boolean atLeastOne = false;
		Set<Type> types = state.getState().getRuntimeTypesOf(expr, this, state.getState());

		for (Type recType : types)
			if (recType.isPointerType()) {
				Type inner = recType.asPointerType().getInnerType();
				if (!inner.isUnitType())
					continue;

				HeapDereference container = new HeapDereference(inner, expr, loc);
				CompilationUnit unit = inner.asUnitType().getUnit();

				Set<CompilationUnit> seen = new HashSet<>();
				HierarcyTraversalStrategy strategy = getProgram().getFeatures().getTraversalStrategy();
				for (CompilationUnit cu : strategy.traverse(this, unit))
					if (seen.add(unit)) {
						Global global = cu.getInstanceGlobal(target, false);
						if (global != null) {
							Variable var = global.toSymbolicVariable(loc);
							AccessChild access = new AccessChild(var.getStaticType(), container, var, loc);
							result = result.lub(state.smallStepSemantics(access, this));
							atLeastOne = true;
						}
					}
			}

		if (atLeastOne)
			return result;

		// worst case: we are accessing a global that we know nothing about
		Set<Type> rectypes = new HashSet<>();
		for (Type t : types)
			if (t.isPointerType())
				rectypes.add(t.asPointerType().getInnerType());

		if (rectypes.isEmpty())
			return state.bottom();

		Type rectype = Type.commonSupertype(rectypes, Untyped.INSTANCE);
		Variable var = new Variable(Untyped.INSTANCE, target, new Annotations(), getLocation());
		HeapDereference container = new HeapDereference(rectype, expr, getLocation());
		AccessChild access = new AccessChild(Untyped.INSTANCE, container, var, getLocation());
		return state.smallStepSemantics(access, this);
	}
}
