package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.program.CodeElement;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.util.datastructures.graph.Node;
import java.util.Objects;

/**
 * A statement of the program to analyze.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Statement extends CodeElement implements Node<Statement, Edge, CFG>, ProgramPoint {

	/**
	 * The cfg containing this statement.
	 */
	private final CFG cfg;

	/**
	 * The offset of the statement within the cfg.
	 */
	protected int offset;

	/**
	 * Builds a statement happening at the given source location.
	 * 
	 * @param cfg        the cfg that this statement belongs to
	 * @param sourceFile the source file where this statement happens. If
	 *                       unknown, use {@code null}
	 * @param line       the line number where this statement happens in the
	 *                       source file. If unknown, use {@code -1}
	 * @param col        the column where this statement happens in the source
	 *                       file. If unknown, use {@code -1}
	 */
	protected Statement(CFG cfg, String sourceFile, int line, int col) {
		super(sourceFile, line, col);
		Objects.requireNonNull(cfg, "Containing CFG cannot be null");
		this.cfg = cfg;
		this.offset = -1;
	}

	@Override
	public final CFG getCFG() {
		return cfg;
	}

	/**
	 * Yields the offset of this statement relative to its containing cfg.
	 * 
	 * @return the offset
	 */
	public final int getOffset() {
		return offset;
	}

	/**
	 * Whether or not this statement stops the execution of the containing cfg,
	 * either by throwing an error or returning a value. To distinguish
	 * error-raising halting statements and normal ones, use
	 * {@link #throwsError()}.
	 * 
	 * @return {@code true} only if that condition holds
	 */
	public boolean stopsExecution() {
		return false;
	}

	/**
	 * Whether or not this statement throws an error, halting the normal
	 * execution of the containing cfg.
	 * 
	 * @return {@code true} only if that condition holds
	 */
	public boolean throwsError() {
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		return prime * result;
	}

	/**
	 * All statements use reference equality for equality checks, to allow
	 * different statement with the same content but placed in different part of
	 * the cfg to being not equal if there are no debug information available.
	 * For checking if two statements are effectively equal (that is, they are
	 * different object with the same structure) use
	 * {@link #isEqualTo(Statement)}. <br>
	 * <br>
	 * {@inheritDoc}
	 */
	@Override
	public final boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public boolean isEqualTo(Statement st) {
		if (this == st)
			return true;
		if (st == null)
			return false;
		if (getClass() != st.getClass())
			return false;
		if (!super.equals(st)) // checking source code location
			return false;
		return true;
	}

	@Override
	public abstract String toString();

	/**
	 * Computes the semantics of the statement, expressing how semantic
	 * information is transformed by the execution of this statement. This
	 * method is also responsible for recursively invoking the
	 * {@link #semantics(AnalysisState, CallGraph, StatementStore)} of each
	 * nested {@link Expression}, saving the result of each call in
	 * {@code expressions}.
	 * 
	 * @param <A>         the type of {@link AbstractState}
	 * @param <H>         the type of the {@link HeapDomain}
	 * @param <V>         the type of the {@link ValueDomain}
	 * @param entryState  the entry state that represents the abstract values of
	 *                        each program variable and memory location when the
	 *                        execution reaches this statement
	 * @param callGraph   the call graph of the program to analyze
	 * @param expressions the cache where analysis states of intermediate
	 *                        expressions must be stored
	 * 
	 * @return the {@link AnalysisState} representing the abstract result of the
	 *             execution of this statement
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public abstract <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> semantics(
					AnalysisState<A, H, V> entryState, CallGraph callGraph, StatementStore<A, H, V> expressions)
					throws SemanticException;
}
