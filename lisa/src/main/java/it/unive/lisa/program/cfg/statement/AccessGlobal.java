package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

/**
 * An access to a {@link Global} of a {@link Unit}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AccessGlobal extends Expression {

	/**
	 * The receiver of the access
	 */
	private final Unit container;

	/**
	 * The global being accessed
	 */
	private final Global target;

	/**
	 * Builds the global access. The location where this access happens is
	 * unknown (i.e. no source file/line/column is available) and its type is
	 * the one of the accessed global.
	 * 
	 * @param cfg       the cfg that this expression belongs to
	 * @param container the unit containing the accessed global
	 * @param target    the accessed global
	 */
	public AccessGlobal(CFG cfg, Unit container, Global target) {
		this(cfg, null, -1, -1, container, target);
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
	 * @param container  the unit containing the accessed global
	 * @param target     the accessed global
	 */
	public AccessGlobal(CFG cfg, String sourceFile, int line, int col, Unit container, Global target) {
		super(cfg, sourceFile, line, col, target.getStaticType());
		this.container = container;
		this.target = target;
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
		return container.getName() + "::" + target.getName();
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> semantics(AnalysisState<A, H, V> entryState,
					CallGraph callGraph, StatementStore<A, H, V> expressions) throws SemanticException {
		// unit globals are unique, we can directly access those
		return entryState.smallStepSemantics(new HeapReference(getRuntimeTypes(), toString()), this);
	}

}
