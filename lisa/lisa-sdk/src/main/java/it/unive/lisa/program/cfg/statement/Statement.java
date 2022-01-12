package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.util.datastructures.graph.Node;
import java.util.Objects;

/**
 * A statement of the program to analyze.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Statement implements Node<Statement, Edge, CFG>, ProgramPoint, Comparable<Statement> {

	/**
	 * The cfg containing this statement.
	 */
	private final CFG cfg;

	/**
	 * The offset of the statement within the cfg.
	 */
	protected int offset;

	private final CodeLocation location;

	/**
	 * Builds a statement happening at the given source location.
	 * 
	 * @param cfg      the cfg that this statement belongs to
	 * @param location the location where this statement is defined within the
	 *                     program
	 */
	protected Statement(CFG cfg, CodeLocation location) {
		Objects.requireNonNull(cfg, "Containing CFG cannot be null");
		Objects.requireNonNull(location, "The location of a statement cannot be null");
		this.cfg = cfg;
		this.location = location;
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
		int result = 1;
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Statement st = (Statement) obj;
		if (location == null) {
			if (st.location == null)
				return true;
		} else if (!location.equals(st.location))
			return false;
		return true;
	}

	@Override
	public abstract String toString();

	/**
	 * Computes the semantics of the statement, expressing how semantic
	 * information is transformed by the execution of this statement. This
	 * method is also responsible for recursively invoking the
	 * {@link #semantics(AnalysisState, InterproceduralAnalysis, StatementStore)}
	 * of each nested {@link Expression}, saving the result of each call in
	 * {@code expressions}.
	 * 
	 * @param <A>             the type of {@link AbstractState}
	 * @param <H>             the type of the {@link HeapDomain}
	 * @param <V>             the type of the {@link ValueDomain}
	 * @param entryState      the entry state that represents the abstract
	 *                            values of each program variable and memory
	 *                            location when the execution reaches this
	 *                            statement
	 * @param interprocedural the interprocedural analysis of the program to
	 *                            analyze
	 * @param expressions     the cache where analysis states of intermediate
	 *                            expressions must be stored
	 *
	 * @return the {@link AnalysisState} representing the abstract result of the
	 *             execution of this statement
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public abstract <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> semantics(
					AnalysisState<A, H, V> entryState, InterproceduralAnalysis<A, H, V> interprocedural,
					StatementStore<A, H, V> expressions)
					throws SemanticException;

	@Override
	public CodeLocation getLocation() {
		return location;
	}

	@Override
	public int compareTo(Statement o) {
		return location.compareTo(o.location);
	}
}
