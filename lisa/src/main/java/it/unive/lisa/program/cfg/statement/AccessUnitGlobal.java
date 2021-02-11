package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.ValueIdentifier;
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
	 * Builds the global access. The location where this access happens is
	 * unknown (i.e. no source file/line/column is available) and its type is
	 * the one of the accessed global.
	 * 
	 * @param cfg      the cfg that this expression belongs to
	 * @param receiver the expression that determines the accessed instance
	 * @param target   the accessed global
	 */
	public AccessUnitGlobal(CFG cfg, Expression receiver, Global target) {
		this(cfg, null, -1, -1, receiver, target);
	}

	/**
	 * Builds the global access, happening at the given location in the program.
	 * The type of this expression is the one of the accessed global.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param sourceFile the source file where this expression happens. If
	 *                       unknown, use {@code null}
	 * @param line       the line number where this expression happens in the
	 *                       source file. If unknown, use {@code -1}
	 * @param col        the column where this expression happens in the source
	 *                       file. If unknown, use {@code -1}
	 * @param receiver   the expression that determines the accessed instance
	 * @param target     the accessed global
	 */
	public AccessUnitGlobal(CFG cfg, String sourceFile, int line, int col, Expression receiver, Global target) {
		super(cfg, sourceFile, line, col, target.getStaticType());
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

	private SymbolicExpression getVariable() {
		SymbolicExpression expr;
		if (target.getStaticType().isPointerType())
			// the smallStepSemantics will take care of converting that
			// reference to a variable identifier
			// setting also the identifier as computed expression
			expr = new HeapReference(getRuntimeTypes(), target.getName());
		else
			expr = new ValueIdentifier(getRuntimeTypes(), target.getName());
		return expr;
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> semantics(AnalysisState<A, H, V> entryState,
					CallGraph callGraph, StatementStore<A, H, V> expressions) throws SemanticException {
		AnalysisState<A, H, V> rec = receiver.semantics(entryState, callGraph, expressions);
		expressions.put(receiver, rec);

		AnalysisState<A, H, V> result = null;
		for (SymbolicExpression expr : rec.getComputedExpressions()) {
			AnalysisState<A, H,
					V> tmp = rec.smallStepSemantics(new AccessChild(getRuntimeTypes(), expr, getVariable()), this);
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
