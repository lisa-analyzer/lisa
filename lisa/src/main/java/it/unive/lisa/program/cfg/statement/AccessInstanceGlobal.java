package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

/**
 * An access to an instance {@link Global} of a {@link CompilationUnit}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AccessInstanceGlobal extends Expression {

	/**
	 * The receiver of the access
	 */
	private final Expression receiver;

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
	 *                     source file, if unknown use {@code null}
	 * @param receiver the expression that determines the accessed instance
	 * @param target   the accessed global
	 */
	public AccessInstanceGlobal(CFG cfg, CodeLocation location, Expression receiver, Global target) {
		super(cfg, location, target.getStaticType());
		this.receiver = receiver;
		this.target = target;
		receiver.setParentStatement(this);
	}

	@Override
	public int setOffset(int offset) {
		return this.offset = offset;
	}

	@Override
	public <V> boolean accept(GraphVisitor<CFG, Statement, Edge, V> visitor, V tool) {
		return visitor.visit(tool, getCFG(), this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((receiver == null) ? 0 : receiver.hashCode());
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
		if (receiver == null) {
			if (other.receiver != null)
				return false;
		} else if (!receiver.equals(other.receiver))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return receiver + "::" + target.getName();
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> semantics(AnalysisState<A, H, V> entryState,
					InterproceduralAnalysis<A, H, V> interprocedural, StatementStore<A, H, V> expressions)
					throws SemanticException {
		AnalysisState<A, H, V> rec = receiver.semantics(entryState, interprocedural, expressions);
		expressions.put(receiver, rec);

		AnalysisState<A, H, V> result = entryState.bottom();
		Variable v = new Variable(getRuntimeTypes(), target.getName(), target.getAnnotations(), target.getLocation());
		for (SymbolicExpression expr : rec.getComputedExpressions()) {
			AnalysisState<A, H, V> tmp = rec.smallStepSemantics(
					new AccessChild(getRuntimeTypes(), new HeapDereference(getRuntimeTypes(), expr, getLocation()), v,
							getLocation()),
					this);
			result = result.lub(tmp);
		}

		if (!receiver.getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(receiver.getMetaVariables());
		return result;
	}
}
