package it.unive.lisa.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.impl.types.InferredTypes;
import it.unive.lisa.analysis.impl.types.TypeEnvironment;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.util.datastructures.graph.Node;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * A statement of the program to analyze.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Statement implements Comparable<Statement>, Node<Statement> {

	/**
	 * The cfg containing this statement.
	 */
	private final CFG cfg;

	/**
	 * The source file where this statement happens. If it is unknown, this
	 * field might contain {@code null}.
	 */
	private final String sourceFile;

	/**
	 * The line where this statement happens in the source file. If it is
	 * unknown, this field might contain {@code -1}.
	 */
	private final int line;

	/**
	 * The column where this statement happens in the source file. If it is
	 * unknown, this field might contain {@code -1}.
	 */
	private final int col;

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
		Objects.requireNonNull(cfg, "Containing CFG cannot be null");
		this.cfg = cfg;
		this.sourceFile = sourceFile;
		this.line = line;
		this.col = col;
		this.offset = -1;
	}

	/**
	 * Yields the CFG that this statement belongs to.
	 * 
	 * @return the containing CFG
	 */
	public final CFG getCFG() {
		return cfg;
	}

	/**
	 * Yields the source file name where this statement happens. This method
	 * returns {@code null} if the source file is unknown.
	 * 
	 * @return the source file, or {@code null}
	 */
	public final String getSourceFile() {
		return sourceFile;
	}

	/**
	 * Yields the line number where this statement happens in the source file.
	 * This method returns {@code -1} if the line number is unknown.
	 * 
	 * @return the line number, or {@code -1}
	 */
	public final int getLine() {
		return line;
	}

	/**
	 * Yields the column where this statement happens in the source file. This
	 * method returns {@code -1} if the line number is unknown.
	 * 
	 * @return the column, or {@code -1}
	 */
	public final int getCol() {
		return col;
	}

	/**
	 * Yields the offset of this statement relative to its containing cfg.
	 * 
	 * @return the offset
	 */
	public final int getOffset() {
		return offset;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + col;
		result = prime * result + line;
		result = prime * result + ((sourceFile == null) ? 0 : sourceFile.hashCode());
		return result;
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
		if (col != st.col)
			return false;
		if (line != st.line)
			return false;
		if (sourceFile == null) {
			if (st.sourceFile != null)
				return false;
		} else if (!sourceFile.equals(st.sourceFile))
			return false;
		return true;
	}

	/**
	 * Compares two statements in terms of order of appearance in the program,
	 * comparing source files first, followed by lines and columns at last. <br>
	 * <br>
	 * {@inheritDoc}
	 */
	@Override
	public final int compareTo(Statement o) {
		int cmp;

		if ((cmp = StringUtils.compare(sourceFile, o.sourceFile)) != 0)
			return cmp;

		if ((cmp = Integer.compare(line, o.line)) != 0)
			return cmp;

		return Integer.compare(col, o.col);
	}

	@Override
	public abstract String toString();

	/**
	 * Computes the runtime types for this statement, expressing how type
	 * information is transformed by the execution of this statement. This
	 * method is also responsible for recursively invoking the
	 * {@link #typeInference(AnalysisState, CallGraph, StatementStore)} of each
	 * nested {@link Expression}, saving the result of each call in
	 * {@code expressions}. If this statement is an {@link Expression},
	 * implementers of this method should call
	 * {@link Expression#setRuntimeTypes(it.unive.lisa.util.collections.ExternalSet)}
	 * with the computed set of {@link Type}s embedded in {@link InferredTypes}
	 * as parameter, in order to register the computed runtime types in the
	 * expression.
	 * 
	 * @param <H>         the concrete type of {@link HeapDomain} that is run
	 *                        during the type inference
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
	public abstract <A extends AbstractState<A, H, TypeEnvironment>,
			H extends HeapDomain<H>> AnalysisState<A, H, TypeEnvironment> typeInference(
					AnalysisState<A, H, TypeEnvironment> entryState, CallGraph callGraph,
					StatementStore<A, H, TypeEnvironment> expressions)
					throws SemanticException;

	/**
	 * Computes the semantics of the statement, expressing how semantic
	 * information is transformed by the execution of this statement. This
	 * method is also responsible for recursively invoking the
	 * {@link #semantics(AnalysisState, CallGraph, StatementStore)} of each
	 * nested {@link Expression}, saving the result of each call in
	 * {@code expressions}.
	 * 
	 * @param <H>         the type of the heap analysis
	 * @param <V>         the type of the value analysis
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
