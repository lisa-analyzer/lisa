package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.util.datastructures.graph.code.CodeNode;
import java.util.Objects;

/**
 * A statement of the program to analyze.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Statement
		implements
		CodeNode<CFG, Statement, Edge>,
		ProgramPoint {

	/**
	 * The cfg containing this statement.
	 */
	private final CFG cfg;

	/**
	 * The location where this statement happens.
	 */
	private final CodeLocation location;

	/**
	 * Builds a statement happening at the given source location.
	 * 
	 * @param cfg      the cfg that this statement belongs to
	 * @param location the location where this statement is defined within the
	 *                     program
	 */
	protected Statement(
			CFG cfg,
			CodeLocation location) {
		Objects.requireNonNull(cfg, "Containing CFG cannot be null");
		Objects.requireNonNull(location, "The location of a statement cannot be null");
		this.cfg = cfg;
		this.location = location;
	}

	@Override
	public final CFG getCFG() {
		return cfg;
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
	public boolean equals(
			Object obj) {
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
	 * Computes the forward semantics of the statement, expressing how semantic
	 * information is transformed by the execution of this statement. This
	 * method is also responsible for recursively invoking the
	 * {@link #forwardSemantics(AnalysisState, InterproceduralAnalysis, StatementStore)}
	 * of each nested {@link Expression}, saving the result of each call in
	 * {@code expressions}.
	 * 
	 * @param <A>             the kind of {@link AbstractLattice} produced by
	 *                            the domain {@code D}
	 * @param <D>             the kind of {@link AbstractDomain} to run during
	 *                            the analysis
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
	public abstract <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> forwardSemantics(
					AnalysisState<A> entryState,
					InterproceduralAnalysis<A, D> interprocedural,
					StatementStore<A> expressions)
					throws SemanticException;

	/**
	 * Computes the backward semantics of the statement, expressing how semantic
	 * information is transformed by the execution of this statement. This
	 * method is also responsible for recursively invoking the
	 * {@link #forwardSemantics(AnalysisState, InterproceduralAnalysis, StatementStore)}
	 * of each nested {@link Expression}, saving the result of each call in
	 * {@code expressions}. By default, this method delegates to
	 * {@link #forwardSemantics(AnalysisState, InterproceduralAnalysis, StatementStore)},
	 * as it is fine for most atomic statements. One should redefine this method
	 * if a statement's semantics is composed of a series of smaller operations.
	 * 
	 * @param <A>             the kind of {@link AbstractLattice} produced by
	 *                            the domain {@code D}
	 * @param <D>             the kind of {@link AbstractDomain} to run during
	 *                            the analysis
	 * @param exitState       the exit state that represents the abstract values
	 *                            of each program variable and memory location
	 *                            when the execution reaches this statement
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
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> backwardSemantics(
					AnalysisState<A> exitState,
					InterproceduralAnalysis<A, D> interprocedural,
					StatementStore<A> expressions)
					throws SemanticException {
		return forwardSemantics(exitState, interprocedural, expressions);
	}

	@Override
	public CodeLocation getLocation() {
		return location;
	}

	@Override
	public final int compareTo(
			Statement o) {
		int cmp;
		if ((cmp = location.compareTo(o.location)) != 0)
			return cmp;
		if ((cmp = getClass().getName().compareTo(o.getClass().getName())) != 0)
			return cmp;
		return compareSameClass(o);
	}

	/**
	 * Auxiliary method for {@link #compareTo(Statement)} that can safely assume
	 * that the two statements happen at the same {@link CodeLocation} and are
	 * instances of the same class.
	 * 
	 * @param o the other statement
	 * 
	 * @return a negative integer, zero, or a positive integer as this object is
	 *             less than, equal to, or greater than the specified object
	 */
	protected abstract int compareSameClass(
			Statement o);

	/**
	 * Yields the {@link Statement} that is evaluated right before this one,
	 * such that querying for the entry state of {@code this} statement is
	 * equivalent to querying the exit state of the returned one. If this method
	 * returns {@code null}, then this is the first expression evaluated when an
	 * entire statement is evaluated.
	 * 
	 * @return the previous statement, or {@code null}
	 */
	public final Statement getEvaluationPredecessor() {
		if (this instanceof Call) {
			Call original = (Call) this;
			while (original.getSource() != null)
				original = original.getSource();
			if (original != this)
				return getStatementEvaluatedBefore(original);
		}

		return getStatementEvaluatedBefore(this);
	}

	/**
	 * Yields the {@link Statement} that precedes the given one, assuming that
	 * {@code other} is contained into this statement. If this method returns
	 * {@code null}, then {@code other} is the first expression evaluated when
	 * this statement is evaluated.
	 *
	 * @param other the other statement
	 * 
	 * @return the previous statement, or {@code null}
	 */
	public Statement getStatementEvaluatedBefore(
			Statement other) {
		return null;
	}

	/**
	 * Yields the {@link Statement} that is evaluated right after this one, such
	 * that querying for the exit state of {@code this} statement is equivalent
	 * to querying the entry state of the returned one. If this method returns
	 * {@code null}, then this is either a statement or the last expression
	 * evaluated when an entire statement is evaluated.
	 * 
	 * @return the next statement, or {@code null}
	 */
	public final Statement getEvaluationSuccessor() {
		if (this instanceof Call) {
			Call original = (Call) this;
			while (original.getSource() != null)
				original = original.getSource();
			if (original != this)
				return getStatementEvaluatedAfter(original);
		}

		return getStatementEvaluatedAfter(this);
	}

	/**
	 * Yields the {@link Statement} that follows the given one, assuming that
	 * {@code other} is contained into this statement. If this method returns
	 * {@code null}, then {@code other} is the last expression evaluated when
	 * this statement is evaluated.
	 *
	 * @param other the other statement
	 * 
	 * @return the next statement, or {@code null}
	 */
	public Statement getStatementEvaluatedAfter(
			Statement other) {
		return null;
	}

}
