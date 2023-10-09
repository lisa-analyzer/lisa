package it.unive.lisa.analysis;

import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * A control flow graph, that has {@link Statement}s as nodes and {@link Edge}s
 * as edges. It also maps each statement (and its inner expressions) to the
 * result of a fixpoint computation, in the form of an {@link AnalysisState}
 * instance.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState} contained into the analysis
 *                state
 */
public class AnalyzedCFG<A extends AbstractState<A>>
		extends
		CFG
		implements
		BaseLattice<AnalyzedCFG<A>> {

	/**
	 * Error message for the inability to lub two graphs.
	 */
	protected static final String CANNOT_LUB_ERROR = "Cannot lub two graphs with different descriptor or different IDs";

	/**
	 * Error message for the inability to glb two graphs.
	 */
	protected static final String CANNOT_GLB_ERROR = "Cannot glb two graphs with different descriptor or different IDs";

	/**
	 * Error message for the inability to widen two graphs.
	 */
	protected static final String CANNOT_WIDEN_ERROR = "Cannot widen two graphs with different descriptor or different IDs";

	/**
	 * Error message for the inability to narrow two graphs.
	 */
	protected static final String CANNOT_NARROW_ERROR = "Cannot perform narrow two graphs with different descriptor or different IDs";

	/**
	 * Error message for the inability to compare two graphs.
	 */
	protected static final String CANNOT_COMPARE_ERROR = "Cannot compare two graphs with different descriptor or different IDs";

	/**
	 * The map storing the analysis results.
	 */
	protected final StatementStore<A> results;

	/**
	 * The map storing the entry state of each entry point.
	 */
	protected final StatementStore<A> entryStates;

	/**
	 * An id meant to identify this specific result, based on how it has been
	 * produced.
	 */
	protected final ScopeId id;

	/**
	 * Builds the control flow graph, storing the given mapping between nodes
	 * and fixpoint computation results.
	 * 
	 * @param cfg       the original control flow graph
	 * @param id        a {@link ScopeId} meant to identify this specific result
	 *                      based on how it has been produced
	 * @param singleton an instance of the {@link AnalysisState} containing the
	 *                      abstract state of the analysis that was executed,
	 *                      used to retrieve top and bottom values
	 */
	public AnalyzedCFG(
			CFG cfg,
			ScopeId id,
			AnalysisState<A> singleton) {
		this(cfg, id, singleton, Collections.emptyMap(), Collections.emptyMap());
	}

	/**
	 * Builds the control flow graph, storing the given mapping between nodes
	 * and fixpoint computation results.
	 * 
	 * @param cfg         the original control flow graph
	 * @param id          a {@link ScopeId} meant to identify this specific
	 *                        result based on how it has been produced
	 * @param singleton   an instance of the {@link AnalysisState} containing
	 *                        the abstract state of the analysis that was
	 *                        executed, used to retrieve top and bottom values
	 * @param entryStates the entry state for each entry point of the cfg
	 * @param results     the results of the fixpoint computation
	 */
	public AnalyzedCFG(
			CFG cfg,
			ScopeId id,
			AnalysisState<A> singleton,
			Map<Statement, AnalysisState<A>> entryStates,
			Map<Statement, AnalysisState<A>> results) {
		super(cfg);
		this.results = new StatementStore<>(singleton);
		results.forEach(this.results::put);
		this.entryStates = new StatementStore<>(singleton);
		entryStates.forEach(this.entryStates::put);
		this.id = id;
	}

	/**
	 * Builds the control flow graph, storing the given mapping between nodes
	 * and fixpoint computation results.
	 * 
	 * @param cfg         the original control flow graph
	 * @param id          a {@link ScopeId} meant to identify this specific
	 *                        result based on how it has been produced
	 * @param entryStates the entry state for each entry point of the cfg
	 * @param results     the results of the fixpoint computation
	 */
	public AnalyzedCFG(
			CFG cfg,
			ScopeId id,
			StatementStore<A> entryStates,
			StatementStore<A> results) {
		super(cfg);
		this.results = results;
		this.entryStates = entryStates;
		this.id = id;
	}

	/**
	 * Yields an id meant to identify this specific result, based on how it has
	 * been produced. This method might return {@code null}.
	 * 
	 * @return the identifier of this result
	 */
	public ScopeId getId() {
		return id;
	}

	/**
	 * Yields the computed result before a given statement (entry state).
	 *
	 * @param st the statement
	 *
	 * @return the result computed before the given statement
	 * 
	 * @throws SemanticException if the lub operator fails
	 */
	public AnalysisState<A> getAnalysisStateBefore(
			Statement st)
			throws SemanticException {
		if (st instanceof Call) {
			Call original = (Call) st;
			while (original.getSource() != null)
				original = original.getSource();
			st = original;
		}

		if (!(st instanceof Expression) || ((Expression) st).getParentStatement() == null)
			if (getEntrypoints().contains(st))
				return entryStates.getState(st);
			else
				return lub(predecessorsOf(st), false);

		// st is not a statement
		// st is not a root-level expression
		Statement pred = st.getEvaluationPredecessor();
		if (pred != null)
			return results.getState(pred);

		// last chance: there is no predecessor, so it might be an entry point
		// of the analysis
		Statement root = ((Expression) st).getRootStatement();
		if (getEntrypoints().contains(root))
			return entryStates.getState(root);

		return entryStates.lattice.bottom();
	}

	/**
	 * Yields the computed result at a given statement (exit state).
	 *
	 * @param st the statement
	 *
	 * @return the result computed at the given statement
	 */
	public AnalysisState<A> getAnalysisStateAfter(
			Statement st) {
		if (st instanceof Call) {
			Call original = (Call) st;
			while (original.getSource() != null)
				original = original.getSource();
			st = original;
		}

		return results.getState(st);
	}

	/**
	 * Yields the entry state.
	 * 
	 * @return the entry state of the CFG
	 * 
	 * @throws SemanticException if the lub operator fails
	 */
	public AnalysisState<A> getEntryState() throws SemanticException {
		return lub(this.getEntrypoints(), true);
	}

	/**
	 * Yields the exit state.
	 * 
	 * @return the entry state of the CFG
	 * 
	 * @throws SemanticException if the lub operator fails
	 */
	public AnalysisState<A> getExitState() throws SemanticException {
		return lub(this.getNormalExitpoints(), false);
	}

	private AnalysisState<A> lub(
			Collection<Statement> statements,
			boolean entry)
			throws SemanticException {
		AnalysisState<A> result = entryStates.lattice.bottom();
		for (Statement st : statements)
			result = result.lub(entry ? getAnalysisStateBefore(st) : getAnalysisStateAfter(st));
		return result;
	}

	@Override
	public AnalyzedCFG<A> lubAux(
			AnalyzedCFG<A> other)
			throws SemanticException {
		if (!getDescriptor().equals(other.getDescriptor()) || !sameIDs(other))
			throw new SemanticException(CANNOT_LUB_ERROR);

		return new AnalyzedCFG<>(
				this,
				id,
				entryStates.lub(other.entryStates),
				results.lub(other.results));
	}

	@Override
	public AnalyzedCFG<A> glbAux(
			AnalyzedCFG<A> other)
			throws SemanticException {
		if (!getDescriptor().equals(other.getDescriptor()) || !sameIDs(other))
			throw new SemanticException(CANNOT_GLB_ERROR);

		return new AnalyzedCFG<>(
				this,
				id,
				entryStates.glb(other.entryStates),
				results.glb(other.results));
	}

	@Override
	public AnalyzedCFG<A> wideningAux(
			AnalyzedCFG<A> other)
			throws SemanticException {
		if (!getDescriptor().equals(other.getDescriptor()) || !sameIDs(other))
			throw new SemanticException(CANNOT_WIDEN_ERROR);

		return new AnalyzedCFG<>(
				this,
				id,
				entryStates.widening(other.entryStates),
				results.widening(other.results));
	}

	@Override
	public AnalyzedCFG<A> narrowingAux(
			AnalyzedCFG<A> other)
			throws SemanticException {
		if (!getDescriptor().equals(other.getDescriptor()) || !sameIDs(other))
			throw new SemanticException(CANNOT_NARROW_ERROR);

		return new AnalyzedCFG<>(
				this,
				id,
				entryStates.narrowing(other.entryStates),
				results.narrowing(other.results));
	}

	@Override
	public boolean lessOrEqualAux(
			AnalyzedCFG<A> other)
			throws SemanticException {
		if (!getDescriptor().equals(other.getDescriptor()) || !sameIDs(other))
			throw new SemanticException(CANNOT_COMPARE_ERROR);

		return entryStates.lessOrEqual(other.entryStates) && results.lessOrEqual(other.results);
	}

	/**
	 * Yields whether or not the {@link #id} of this graph and the given one are
	 * the same.
	 * 
	 * @param other the other graph
	 * 
	 * @return {@code true} if that condition holds
	 */
	protected boolean sameIDs(
			AnalyzedCFG<A> other) {
		if (id == null) {
			if (other.id == null)
				return true;
			return false;
		} else if (other.id == null)
			return false;
		else
			return id.equals(other.id);
	}

	@Override
	public AnalyzedCFG<A> top() {
		return new AnalyzedCFG<>(this, id.startingId(), entryStates.top(), results.top());
	}

	@Override
	public boolean isTop() {
		return entryStates.isTop() && results.isTop();
	}

	@Override
	public AnalyzedCFG<A> bottom() {
		return new AnalyzedCFG<>(this, id.startingId(), entryStates.bottom(), results.bottom());
	}

	@Override
	public boolean isBottom() {
		return entryStates.isBottom() && results.isBottom();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entryStates == null) ? 0 : entryStates.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((results == null) ? 0 : results.hashCode());
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
		AnalyzedCFG<?> other = (AnalyzedCFG<?>) obj;
		if (entryStates == null) {
			if (other.entryStates != null)
				return false;
		} else if (!entryStates.equals(other.entryStates))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (results == null) {
			if (other.results != null)
				return false;
		} else if (!results.equals(other.results))
			return false;
		return true;
	}

	@Override
	public StructuredRepresentation representation() {
		throw new UnsupportedOperationException();
	}
}
