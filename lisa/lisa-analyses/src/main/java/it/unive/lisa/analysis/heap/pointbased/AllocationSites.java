package it.unive.lisa.analysis.heap.pointbased;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.lattices.SetLattice;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.heap.NonRelationalHeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A heap domain tracking sets of {@link AllocationSite}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class AllocationSites extends SetLattice<AllocationSites, AllocationSite>
		implements
		NonRelationalHeapDomain<AllocationSites> {

	private static final AllocationSites TOP = new AllocationSites(new HashSet<>(), true);
	private static final AllocationSites BOTTOM = new AllocationSites(new HashSet<>(), false);

	/**
	 * Builds an instance of HeapIdentiferSetLattice, corresponding to the top
	 * element.
	 */
	public AllocationSites() {
		this(new HashSet<>(), true);
	}

	/**
	 * Builds an instance of this class to hold the given sites.
	 * 
	 * @param set   the set of {@link AllocationSite}s
	 * @param isTop whether this instance is the top of the lattice
	 */
	AllocationSites(
			Set<AllocationSite> set,
			boolean isTop) {
		super(set, isTop);
	}

	@Override
	public AllocationSites top() {
		return TOP;
	}

	@Override
	public AllocationSites bottom() {
		return BOTTOM;
	}

	@Override
	public AllocationSites mk(
			Set<AllocationSite> set) {
		return new AllocationSites(set, false);
	}

	@Override
	public Iterator<AllocationSite> iterator() {
		return this.elements.iterator();
	}

	@Override
	public AllocationSites eval(
			SymbolicExpression expression,
			HeapEnvironment<AllocationSites> environment,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return new AllocationSites(Collections.singleton((AllocationSite) expression), false);
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return Collections.emptyList();
	}

	@Override
	public AllocationSites lubAux(
			AllocationSites other)
			throws SemanticException {
		Map<String, AllocationSite> lub = new HashMap<>();

		// all weak identifiers are part of the lub
		elements.stream().filter(AllocationSite::isWeak).forEach(e -> lub.put(e.getName(), e));
		// common ones will be overwritten
		other.elements.stream().filter(AllocationSite::isWeak).forEach(e -> lub.put(e.getName(), e));

		// strong identifiers are only added if we did not consider a
		// weak identifier with the same name
		elements.stream().filter(Predicate.not(AllocationSite::isWeak))
				.filter(e -> !lub.containsKey(e.getName()))
				.forEach(e -> lub.put(e.getName(), e));

		other.elements.stream().filter(Predicate.not(AllocationSite::isWeak))
				.filter(e -> !lub.containsKey(e.getName()))
				.forEach(e -> lub.put(e.getName(), e));

		return new AllocationSites(new HashSet<>(lub.values()), false);
	}

	@Override
	public boolean canProcess(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return expression instanceof AllocationSite;
	}

	@Override
	public HeapEnvironment<AllocationSites> assume(
			HeapEnvironment<AllocationSites> environment,
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return environment;
	}

	@Override
	public ExpressionSet rewrite(
			SymbolicExpression expression,
			HeapEnvironment<AllocationSites> environment,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ExpressionSet();
	}

	/**
	 * Applies a substitution to the contents of this set of allocation sites,
	 * similarly to
	 * {@link ValueDomain#applyReplacement(it.unive.lisa.analysis.heap.HeapSemanticOperation.HeapReplacement, ProgramPoint, SemanticOracle)}.
	 * 
	 * @param r  the substitution
	 * @param pp the program point where the substitution is applied
	 * 
	 * @return a copy of this set of sites where the substitution has been
	 *             applied
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public AllocationSites applyReplacement(
			HeapReplacement r,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || r.getSources().isEmpty())
			return this;

		Set<AllocationSite> copy = new HashSet<>(elements);
		if (copy.removeAll(r.getSources())) {
			r.getTargets().stream()
					.filter(AllocationSite.class::isInstance)
					.map(AllocationSite.class::cast)
					.forEach(copy::add);
			return new AllocationSites(copy, false);
		} else
			return this;
	}

	@Override
	public AllocationSites unknownVariable(
			Identifier id) {
		// we use bottom since heap environments track all possible keys: the
		// absence of a key means no information (bottom) instead of any
		// possible information (top)
		return bottom();
	}

	@Override
	public Satisfiability alias(
			SymbolicExpression x,
			SymbolicExpression y,
			HeapEnvironment<AllocationSites> environment,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability isReachableFrom(
			SymbolicExpression x,
			SymbolicExpression y,
			HeapEnvironment<AllocationSites> environment,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}
}
