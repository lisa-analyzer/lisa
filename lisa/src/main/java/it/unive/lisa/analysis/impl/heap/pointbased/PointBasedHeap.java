package it.unive.lisa.analysis.impl.heap.pointbased;

import static java.util.Collections.singleton;
import static org.apache.commons.collections.CollectionUtils.union;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.BaseHeapDomain;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.util.collections.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A field-insensitive point-based heap implementation that abstracts heap
 * locations depending on their allocation sites, namely the position of the
 * code where heap locations are generated. All heap locations that are
 * generated at the same allocation sites are abstracted into a single unique
 * heap identifier. The implementation follows X. Rival and K. Yi, "Introduction
 * to Static Analysis An Abstract Interpretation Perspective", Section 8.3.4
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.itØ">Vincenzo Arceri</a>
 * 
 * @see <a href=
 *          "https://mitpress.mit.edu/books/introduction-static-analysis">https://mitpress.mit.edu/books/introduction-static-analysis</a>
 */
public class PointBasedHeap extends BaseHeapDomain<PointBasedHeap> {

	private final List<HeapReplacement> substitutions;

	private final Collection<ValueExpression> rewritten;

	/**
	 * An heap environment tracking which allocation sites are associated to
	 * each identifier.
	 */
	protected final HeapEnvironment<AllocationSites> heapEnv;

	/**
	 * Builds a new instance of field-insensitive point-based heap, with an
	 * unique rewritten expression {@link Skip}.
	 */
	public PointBasedHeap() {
		this(singleton(new Skip()), new HeapEnvironment<AllocationSites>(new AllocationSites()),
				Collections.emptyList());
	}

	/**
	 * Builds a new instance of field-insensitive point-based heap from its
	 * rewritten expressions and heap environment.
	 * 
	 * @param rewritten     the collection of rewritten expressions
	 * @param heapEnv       the heap environment that this instance tracks
	 * @param substitutions the list of heap replacement
	 */
	protected PointBasedHeap(Collection<ValueExpression> rewritten,
			HeapEnvironment<AllocationSites> heapEnv, List<HeapReplacement> substitutions) {
		this.rewritten = rewritten;
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

		if (expression instanceof AllocationSite) {
			HeapEnvironment<AllocationSites> heap = heapEnv.assign(id, expression, pp);
			return from(new PointBasedHeap(singleton((AllocationSite) expression),
					applySubstitutions(heap, substitutions), substitutions));
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
		return from(new PointBasedHeap(rewritten, heapEnv.forgetIdentifier(id), substitutions));
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		// we leave the decision to the value domain
		return Satisfiability.UNKNOWN;
	}

	@Override
	public String representation() {
		if (isTop())
			return Lattice.TOP_STRING;

		if (isBottom())
			return Lattice.BOTTOM_STRING;

		Collection<String> res = new TreeSet<String>(
				(l, r) -> Utils.nullSafeCompare(true, l, r, (ll, rr) -> ll.toString().compareTo(rr.toString())));
		for (Identifier id : heapEnv.getKeys())
			for (HeapLocation hid : heapEnv.getState(id))
				res.add(hid.toString());

		return res.toString();
	}

	@Override
	public PointBasedHeap top() {
		return from(new PointBasedHeap(Collections.emptySet(), heapEnv.top(), Collections.emptyList()));
	}

	@Override
	public boolean isTop() {
		return rewritten.isEmpty() && heapEnv.isTop();
	}

	@Override
	public PointBasedHeap bottom() {
		return from(new PointBasedHeap(Collections.emptySet(), heapEnv.bottom(), Collections.emptyList()));
	}

	@Override
	public boolean isBottom() {
		return rewritten.isEmpty() && heapEnv.isBottom();
	}

	@Override
	public Collection<ValueExpression> getRewrittenExpressions() {
		return rewritten;
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return substitutions;
	}

	@Override
	public PointBasedHeap mk(PointBasedHeap reference, ValueExpression expression) {
		return from(new PointBasedHeap(singleton(expression), reference.heapEnv, reference.substitutions));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected PointBasedHeap lubAux(PointBasedHeap other) throws SemanticException {
		ArrayList<HeapReplacement> newSubstitions = new ArrayList<>(substitutions);
		newSubstitions.addAll(other.substitutions);

		return from(new PointBasedHeap(union(rewritten, other.rewritten),
				heapEnv.lub(other.heapEnv), newSubstitions));
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
		result = prime * result + ((rewritten == null) ? 0 : rewritten.hashCode());
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
		if (rewritten == null) {
			if (other.rewritten != null)
				return false;
		} else if (!rewritten.equals(other.rewritten))
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
				if (site.getId().equals(id.getId()))
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

		for (Identifier k : heap.getKeys()) {
			Set<AllocationSite> newSites = new HashSet<>();
			for (AllocationSite l : heap.getState(k))
				if (substitution.stream().noneMatch(t -> t.getSources().contains(l)))
					newSites.add(l);
				else
					for (HeapReplacement replacement : substitution)
						if (replacement.getSources().contains(l))
							for (Identifier target : replacement.getTargets())
								newSites.add((AllocationSite) target);

			map.put(k, new AllocationSites().mk(newSites));
		}

		HeapEnvironment<
				AllocationSites> heapEnvironment = new HeapEnvironment<AllocationSites>(new AllocationSites(), map);
		return heapEnvironment;
	}

	@Override
	protected PointBasedHeap semanticsOf(HeapExpression expression, ProgramPoint pp) throws SemanticException {
		if (expression instanceof AccessChild) {
			PointBasedHeap containerState = smallStepSemantics((((AccessChild) expression).getContainer()), pp);
			PointBasedHeap childState = containerState.smallStepSemantics((((AccessChild) expression).getChild()),
					pp);

			List<HeapReplacement> substitution = new ArrayList<>(childState.substitutions);

			Set<ValueExpression> result = new HashSet<>();
			for (SymbolicExpression exp : containerState.getRewrittenExpressions()) {
				if (exp instanceof Variable && !childState.heapEnv.getState((Variable) exp).isBottom()) {
					AllocationSites expHids = childState.heapEnv.getState((Variable) exp);
					for (AllocationSite hid : expHids) {
						AllocationSite previousAllocated = alreadyAllocated(childState.heapEnv, hid);
						if (previousAllocated == null) {
							result.add(new AllocationSite(expression.getTypes(), hid.getId()));
						} else {
							AllocationSite weak = new AllocationSite(expression.getTypes(), hid.getId(), true);
							AllocationSite strong = new AllocationSite(expression.getTypes(), hid.getId());
							HeapReplacement replacement = new HeapReplacement();
							replacement.addSource(strong);
							replacement.addTarget(weak);
							substitution.add(replacement);
							if (previousAllocated.isWeak())
								result.add(weak);
							else
								result.add(strong);
						}
					}

				} else if (exp instanceof AllocationSite) {
					AllocationSite site = (AllocationSite) exp;
					result.add(new AllocationSite(expression.getTypes(), site.getId(), site.isWeak()));
				} else if (exp instanceof HeapLocation) {
					result.add((ValueExpression) exp);
				}
			}

			return from(new PointBasedHeap(result, applySubstitutions(childState.heapEnv, substitution), substitution));
		}

		if (expression instanceof HeapAllocation) {
			AllocationSite id = new AllocationSite(expression.getTypes(), pp.getLocation().getCodeLocation());
			AllocationSite oldAllocation = alreadyAllocated(heapEnv, id);
			List<HeapReplacement> substitution = new ArrayList<>();
			if (oldAllocation != null && !oldAllocation.isWeak()) {
				id = new AllocationSite(id.getTypes(), id.getId(), true);
				HeapReplacement replacement = new HeapReplacement();
				replacement.addSource(oldAllocation);
				replacement.addTarget(id);
				substitution.add(replacement);
			}

			return from(new PointBasedHeap(singleton(id), applySubstitutions(heapEnv, substitution), substitution));
		}

		return top();
	}
}