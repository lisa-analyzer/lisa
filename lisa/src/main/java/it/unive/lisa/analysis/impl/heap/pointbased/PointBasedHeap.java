package it.unive.lisa.analysis.impl.heap.pointbased;

import static java.util.Collections.singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.BaseHeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.SetRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PointerIdentifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * A field-insensitive point-based heap implementation that abstracts heap
 * locations depending on their allocation sites, namely the position of the
 * code where heap locations are generated. All heap locations that are
 * generated at the same allocation sites are abstracted into a single unique
 * heap identifier. The implementation follows X. Rival and K. Yi, "Introduction
 * to Static Analysis An Abstract Interpretation Perspective", Section 8.3.4
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 * 
 * @see <a href=
 *          "https://mitpress.mit.edu/books/introduction-static-analysis">https://mitpress.mit.edu/books/introduction-static-analysis</a>
 */
public class PointBasedHeap extends BaseHeapDomain<PointBasedHeap> {

	/**
	 * The list of heap replacement
	 */
	private final List<HeapReplacement> substitutions;

	/**
	 * An heap environment tracking which allocation sites are associated to
	 * each identifier.
	 */
	protected final HeapEnvironment<AllocationSites> heapEnv;

	/**
	 * Builds a new instance of field-insensitive point-based heap.
	 */
	public PointBasedHeap() {
		this(new HeapEnvironment<AllocationSites>(new AllocationSites()), Collections.emptyList());
	}

	/**
	 * Builds a new instance of field-insensitive point-based heap from its heap
	 * environment and substitutions.
	 * 
	 * @param heapEnv       the heap environment that this instance tracks
	 * @param substitutions the list of heap replacement
	 */
	protected PointBasedHeap(HeapEnvironment<AllocationSites> heapEnv, List<HeapReplacement> substitutions) {
		this.heapEnv = heapEnv;
		this.substitutions = substitutions;
	}

	/**
	 * Builds a point-based heap from a reference one.
	 * 
	 * @param original reference point-based heap
	 * 
	 * @return a point-based heap build from the original one
	 */
	protected PointBasedHeap from(PointBasedHeap original) {
		return original;
	}

	@Override
	public PointBasedHeap assign(Identifier id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		if (expression instanceof PointerIdentifier) {
			PointerIdentifier pid = (PointerIdentifier) expression;
			HeapEnvironment<AllocationSites> heap = heapEnv.assign(id, pid.getLocation(), pp);
			return from(new PointBasedHeap(applySubstitutions(heap, substitutions), substitutions));
		}

		return smallStepSemantics(expression, pp);
	}

	@Override
	public PointBasedHeap assume(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		// we just rewrite the expression if needed
		return smallStepSemantics(expression, pp);
	}

	@Override
	public PointBasedHeap forgetIdentifier(Identifier id) throws SemanticException {
		return from(new PointBasedHeap(heapEnv.forgetIdentifier(id), substitutions));
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		// we leave the decision to the value domain
		return Satisfiability.UNKNOWN;
	}

	@Override
	public DomainRepresentation representation() {
		if (isTop())
			return Lattice.TOP_REPR;

		if (isBottom())
			return Lattice.BOTTOM_REPR;

		Set<HeapLocation> res = new HashSet<>();
		for (Identifier id : heapEnv.getKeys())
			for (HeapLocation hid : heapEnv.getState(id))
				res.add(hid);

		return new SetRepresentation(res, StringRepresentation::new);
	}

	@Override
	public PointBasedHeap top() {
		return from(new PointBasedHeap(heapEnv.top(), Collections.emptyList()));
	}

	@Override
	public boolean isTop() {
		return heapEnv.isTop();
	}

	@Override
	public PointBasedHeap bottom() {
		return from(new PointBasedHeap(heapEnv.bottom(), Collections.emptyList()));
	}

	@Override
	public boolean isBottom() {
		return heapEnv.isBottom();
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return substitutions;
	}

	@Override
	public PointBasedHeap mk(PointBasedHeap reference) {
		return from(new PointBasedHeap(reference.heapEnv, reference.substitutions));
	}

	@Override
	protected PointBasedHeap lubAux(PointBasedHeap other) throws SemanticException {
		ArrayList<HeapReplacement> newSubstitions = new ArrayList<>(substitutions);
		newSubstitions.addAll(other.substitutions);

		return from(new PointBasedHeap(heapEnv.lub(other.heapEnv), newSubstitions));
	}

	@Override
	protected PointBasedHeap wideningAux(PointBasedHeap other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected boolean lessOrEqualAux(PointBasedHeap other) throws SemanticException {
		return heapEnv.lessOrEqual(other.heapEnv);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((heapEnv == null) ? 0 : heapEnv.hashCode());
		result = prime * result + ((substitutions == null) ? 0 : substitutions.hashCode());
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
		PointBasedHeap other = (PointBasedHeap) obj;
		if (heapEnv == null) {
			if (other.heapEnv != null)
				return false;
		} else if (!heapEnv.equals(other.heapEnv))
			return false;
		if (substitutions == null) {
			if (other.substitutions != null)
				return false;
		} else if (!substitutions.equals(other.substitutions))
			return false;
		return true;
	}

	private AllocationSite alreadyAllocated(HeapEnvironment<AllocationSites> heap, AllocationSite id) {
		for (AllocationSites set : heap.values())
			for (AllocationSite site : set)
				if (site.getName().equals(id.getName()))
					return site;

		return null;
	}

	/**
	 * Applies a substitution of identifiers substitutions in a given heap
	 * environment.
	 * 
	 * @param heap         the heap environment where to apply the substitutions
	 * @param substitution the substitution to apply
	 * 
	 * @return the heap environment modified by the substitution
	 */
	protected HeapEnvironment<AllocationSites> applySubstitutions(HeapEnvironment<AllocationSites> heap,
			List<HeapReplacement> substitution) {
		if (heap.isTop() || heap.isBottom() || substitution == null || substitution.isEmpty())
			return heap;

		Map<Identifier, AllocationSites> map = new HashMap<>();

		for (Entry<Identifier, AllocationSites> entry : heap) {
			Set<AllocationSite> newSites = new HashSet<>();
			for (AllocationSite l : entry.getValue())
				if (substitution.stream().noneMatch(t -> t.getSources().contains(l)))
					newSites.add(l);
				else
					for (HeapReplacement replacement : substitution)
						if (replacement.getSources().contains(l))
							for (Identifier target : replacement.getTargets())
								newSites.add((AllocationSite) target);

			map.put(entry.getKey(), new AllocationSites().mk(newSites));
		}

		return new HeapEnvironment<>(new AllocationSites(), map);
	}

	private HeapReplacement replaceStrong(AllocationSite site, ExternalSet<Type> types) {
		AllocationSite weak = new AllocationSite(types, site.getId(), true);
		AllocationSite strong = new AllocationSite(types, site.getId());
		HeapReplacement replacement = new HeapReplacement();
		replacement.addSource(strong);
		replacement.addTarget(weak);
		return replacement;
	}

	@Override
	protected PointBasedHeap semanticsOf(HeapExpression expression, ProgramPoint pp) throws SemanticException {
		if (expression instanceof AccessChild) {

			AccessChild access = (AccessChild) expression;
			PointBasedHeap childState = smallStepSemantics(access.getChild(), pp);
			List<HeapReplacement> substitution = new ArrayList<>(childState.substitutions);

			AllocationSite site = (AllocationSite) access.getContainer().getLocation();
			AllocationSite newSite = new AllocationSite(access.getTypes(), site.getId(), site.isWeak());

			substitution.add(replaceStrong(newSite, access.getTypes()));

			return from(new PointBasedHeap(applySubstitutions(childState.heapEnv, substitution), substitution));
		}

		if (expression instanceof HeapAllocation) {
			AllocationSite id = new AllocationSite(expression.getTypes(), pp.getLocation().getCodeLocation());
			AllocationSite previousLocation = alreadyAllocated(heapEnv, id);
			List<HeapReplacement> substitution = new ArrayList<>(substitutions);

			if (previousLocation != null)
				if (!previousLocation.isWeak()) {
					id = new AllocationSite(id.getTypes(), id.getId(), true);
					HeapReplacement replacement = new HeapReplacement();
					replacement.addSource(previousLocation);
					replacement.addTarget(id);
					substitution.add(replacement);
				}

			return from(new PointBasedHeap(applySubstitutions(heapEnv, substitution), substitution));
		}

		if (expression instanceof HeapReference || expression instanceof HeapDereference) 
			return from(new PointBasedHeap(heapEnv, substitutions));

		return top();
	}

	@Override
	public ExpressionSet<ValueExpression> rewrite(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return expression.accept(new Rewriter(), pp);
	}

	/**
	 * A {@link it.unive.lisa.analysis.heap.BaseHeapDomain.Rewriter} for the
	 * {@link PointBasedHeap} domain.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	protected class Rewriter extends BaseHeapDomain.Rewriter {

		@Override
		public ExpressionSet<ValueExpression> visit(AccessChild expression, PointerIdentifier receiver,
				ExpressionSet<ValueExpression> child, Object... params) throws SemanticException {
			Set<ValueExpression> result = new HashSet<>();

			AllocationSite site = (AllocationSite) receiver.getLocation();

			result.add(new AllocationSite(expression.getTypes(), site.getId(), true));

			return new ExpressionSet<>(result);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapAllocation expression, Object... params)
				throws SemanticException {
			AllocationSite id = new AllocationSite(expression.getTypes(),
					((ProgramPoint) params[0]).getLocation().getCodeLocation());
			AllocationSite previousLocation = alreadyAllocated(heapEnv, id);

			if (previousLocation != null) {
				if (!previousLocation.isWeak())
					id = new AllocationSite(id.getTypes(), id.getId(), true);
				else
					id = new AllocationSite(expression.getTypes(), previousLocation.getId(), previousLocation.isWeak());
			} else {
				// Check if the allocation site, at that point, has not been
				// already allocated but must be rewritten
				Set<ValueExpression> result = null;
				for (HeapReplacement r : substitutions)
					if (r.getSources().contains(id))
						result = r.getTargets().stream().map(e -> (ValueExpression) e).collect(Collectors.toSet());

				if (result == null)
					result = singleton(id);
				else
					result = result.stream()
					.map(l -> new AllocationSite(expression.getTypes(), ((AllocationSite) l).getId()))
					.collect(Collectors.toSet());
				return new ExpressionSet<>(result);
			}

			return new ExpressionSet<>(id);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapReference expression, Object... params)
				throws SemanticException {
			return new ExpressionSet<>(new PointerIdentifier(expression.getLocation().getTypes(), expression.getLocation()));
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapDereference expression, Object... params)
				throws SemanticException {

			if (expression.getExpression() instanceof Variable) {
				Variable v = (Variable) expression.getExpression();

				if (heapEnv.getKeys().contains(v)) {
					Set<ValueExpression> result = new HashSet<>();

					for (AllocationSite site : heapEnv.getState(v))
						result.add(new PointerIdentifier(site.getTypes(), site));
					return new ExpressionSet<>(result);
				}
			}

			return rewrite(expression.getExpression(), (ProgramPoint) params[0]);
		}
	}
}