package it.unive.lisa.analysis.impl.heap.pointbased;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

		PointBasedHeap sss = smallStepSemantics(expression, pp);
		ExpressionSet<ValueExpression> rewrittenExp = sss.rewrite(expression, pp);

		PointBasedHeap result = bottom();
		for (ValueExpression exp : rewrittenExp) {
			if (exp instanceof PointerIdentifier) {
				PointerIdentifier pid = (PointerIdentifier) exp;	
				Identifier v = id instanceof PointerIdentifier ? ((PointerIdentifier) id).getLocation() : id;
				HeapEnvironment<AllocationSites> heap = sss.heapEnv.assign(v, pid.getLocation(), pp);
				result = result.lub(from(new PointBasedHeap(applySubstitutions(heap, sss.substitutions), sss.substitutions)));
			} else
				result = result.lub(sss);
		}

		
		if (id instanceof AllocationSite) {
			List<HeapReplacement> subs = new ArrayList<>(result.substitutions);
			subs.add(replaceStrong((AllocationSite) id));
			result = from(new PointBasedHeap(applySubstitutions(result.heapEnv, subs), subs));
		}
		
		return result;
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

	private AllocationSite alreadyAllocated(AllocationSite id) {
		for (AllocationSites set : heapEnv.values())
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
								// Preserve the original type, need only to change weakeness
								newSites.add(new AllocationSite(l.getTypes(), ((AllocationSite) target).getId(), target.isWeak()));

			map.put(entry.getKey(), new AllocationSites().mk(newSites));
		}

		return new HeapEnvironment<>(new AllocationSites(), map);
	}

	private HeapReplacement replaceStrong(AllocationSite site) {
		AllocationSite weak = new AllocationSite(site.getTypes(), site.getId(), true);
		AllocationSite strong = new AllocationSite(site.getTypes(), site.getId());
		HeapReplacement replacement = new HeapReplacement();
		replacement.addSource(strong);
		replacement.addTarget(weak);
		return replacement;
	}

	@Override
	protected PointBasedHeap semanticsOf(HeapExpression expression, ProgramPoint pp) throws SemanticException {
		if (expression instanceof AccessChild) {
			AccessChild access = (AccessChild) expression;

			PointBasedHeap containerState = smallStepSemantics(access.getContainer(), pp);
			return containerState.smallStepSemantics(access.getChild(), pp);

		}

		if (expression instanceof HeapAllocation || expression instanceof HeapReference || expression instanceof HeapDereference) 
			return from(new PointBasedHeap(heapEnv, substitutions));

		return top();
	}

	@Override
	public ExpressionSet<ValueExpression> rewrite(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return expression.accept(new Rewriter(), pp);
	}

	private Set<ValueExpression> resolveIdentifier(Identifier v) {
		Set<ValueExpression> result = new HashSet<>();
		for (AllocationSite site : heapEnv.getState(v)) 
			result.add(new PointerIdentifier(site.getTypes(), site));

		return result;
	}

	/**
	 * A {@link it.unive.lisa.analysis.heap.BaseHeapDomain.Rewriter} for the
	 * {@link PointBasedHeap} domain.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	protected class Rewriter extends BaseHeapDomain.Rewriter {

		@Override
		public ExpressionSet<ValueExpression> visit(AccessChild expression, ExpressionSet<ValueExpression> receiver,
				ExpressionSet<ValueExpression> child, Object... params) throws SemanticException {
			Set<ValueExpression> result = new HashSet<>();

			for (ValueExpression rec : receiver) 
				if (rec instanceof PointerIdentifier) {
					PointerIdentifier pid = (PointerIdentifier) rec;
					AllocationSite site = (AllocationSite) pid.getLocation();
					result.add(new AllocationSite(expression.getTypes(), site.getId(), true));
				}

			return new ExpressionSet<>(result);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapAllocation expression, Object... params)
				throws SemanticException {
			AllocationSite id = new AllocationSite(expression.getTypes(),
					((ProgramPoint) params[0]).getLocation().getCodeLocation());

			if (alreadyAllocated(id) != null) 
				return new ExpressionSet<>(new AllocationSite(id.getTypes(), id.getId(), true));
			else
				return new ExpressionSet<>(new AllocationSite(id.getTypes(), id.getId(), false));
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapReference expression, ExpressionSet<ValueExpression> loc, Object... params)
				throws SemanticException {
			Set<ValueExpression> result = new HashSet<>();

			for (ValueExpression locExp : loc)
				if (locExp instanceof AllocationSite) {
					AllocationSite l = (AllocationSite) locExp;
					result.add(new PointerIdentifier(l.getTypes(), l));
				}
			return new ExpressionSet<>(result);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapDereference expression, ExpressionSet<ValueExpression> ref, Object... params)
				throws SemanticException {
			Set<ValueExpression> result = new HashSet<>();

			for(ValueExpression refExp : ref) {
				if (refExp instanceof Identifier && !(refExp instanceof PointerIdentifier)) {
					Identifier id = (Identifier) refExp;
					if (heapEnv.getKeys().contains(id)) 
						result.addAll(resolveIdentifier(id));
				} else 
					for (ValueExpression rew : rewrite(expression.getExpression(), (ProgramPoint) params[0]))
						result.add(rew);
			}

			return new ExpressionSet<>(result); 
		}

		@Override
		public final ExpressionSet<ValueExpression> visit(Identifier expression, Object... params)
				throws SemanticException {
			if (!(expression instanceof PointerIdentifier)) 
				if (heapEnv.getKeys().contains(expression)) 
					return new ExpressionSet<>(resolveIdentifier(expression));

			return new ExpressionSet<>(expression);
		}
	}
}