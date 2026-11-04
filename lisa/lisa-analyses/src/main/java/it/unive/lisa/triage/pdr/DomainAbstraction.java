package it.unive.lisa.triage.pdr;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.triage.model.ConstraintExporter;
import it.unive.lisa.triage.model.HeapModel;
import it.unive.lisa.triage.model.ModelLocation;
import it.unive.lisa.triage.reachability.Reachability;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A {@link CubeAbstraction} driven by the abstract domain of the analysis. The
 * cube is abstracted in the domain by assuming each of its literals, through
 * the semantics of the analysis, on the state computed by the analysis at the
 * location (in the context of its frame): the result over-approximates the states of the cube that satisfy
 * the invariant of the location. The abstract state is then exported as
 * constraints over the identifiers of the cube ({@link ConstraintExporter}).
 * The proposal has thus the shape of the elements of the domain (e.g., bounds
 * for intervals, prefixes for string domains), regardless of the shape of the
 * literals of the cube, and it is independent of the domain at hand: domains
 * that do not generate constraints yield no proposal.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the type of {@link AbstractLattice} produced by the analysis
 * @param <D> the type of {@link AbstractDomain} used by the analysis
 */
public class DomainAbstraction<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		implements
		CubeAbstraction {

	private final Analysis<A, D> analysis;

	private final Map<ModelLocation, AnalysisState<A>> preStates = new LinkedHashMap<>();

	/**
	 * Builds the abstraction.
	 *
	 * @param analysis the analysis that computed the results of the frames
	 */
	public DomainAbstraction(
			Analysis<A, D> analysis) {
		this.analysis = analysis;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Cube abstractCube(
			ModelLocation location,
			Cube cube)
			throws SemanticException {
		AnalyzedCFG<A> result = (AnalyzedCFG<A>) location.getFrame().getResult();
		Statement node = location.getNode();
		AnalysisState<A> state = preStates.get(location);
		if (state == null) {
			state = Reachability.preStateOf(result, node, analysis);
			preStates.put(location, state);
		}
		for (SymbolicExpression lit : cube.getLiterals()) {
			if (HeapModel.mentionsHeapModel(lit))
				// the analysis cannot evaluate the heap model: not assuming the
				// literal only weakens the proposal
				continue;
			state = analysis.assume(state, lit, result.getId(), node, node);
			if (state.getExecution().isBottom())
				// the domain proves that the cube is excluded by the
				// invariant: there is nothing to generalize
				return null;
		}
		Set<Identifier> ids = Projection.identifiers(cube.getLiterals());
		ConstraintExporter<A, D> exporter = new ConstraintExporter<>(analysis.domain, result.getId());
		Set<SymbolicExpression> constraints = exporter.constraints(state, ids, node);
		if (constraints == null || constraints.isEmpty())
			return null;
		return new Cube(constraints);
	}
}
