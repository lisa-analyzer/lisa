package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

/**
 * An access to an instance {@link Global} of a {@link CompilationUnit}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AccessUnitGlobal extends Expression {

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
	public AccessUnitGlobal(CFG cfg, CodeLocation location, Expression receiver, Global target) {
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
	public String toString() {
		return receiver + "::" + target.getName();
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> semantics(
					AnalysisState<A, H, V> entryState, CallGraph callGraph, StatementStore<A, H, V> expressions)
					throws SemanticException {
		AnalysisState<A, H, V> rec = receiver.semantics(entryState, callGraph, expressions);
		expressions.put(receiver, rec);

		AnalysisState<A, H, V> result = null;
		Variable v = new Variable(getRuntimeTypes(), target.getName(), target.getAnnotations());
		for (SymbolicExpression expr : rec.getComputedExpressions()) {
			AnalysisState<A, H, V> tmp = rec.smallStepSemantics(new AccessChild(getRuntimeTypes(), expr, v), this);
			if (result == null)
				result = tmp;
			else
				result = result.lub(tmp);
		}

		if (!receiver.getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(receiver.getMetaVariables());
		return result;
	}

}
