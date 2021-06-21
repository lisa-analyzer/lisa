package it.unive.lisa.analysis.impl.heap.pointbased;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
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
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	 * An heap environment tracking which allocation sites are associated to
	 * each identifier.
	 */
	protected final HeapEnvironment<AllocationSites> heapEnv;

	/**
	 * Builds a new instance of field-insensitive point-based heap.
	 */
	public PointBasedHeap() {
		this(new HeapEnvironment<AllocationSites>(new AllocationSites()));
	}

	/**
	 * Builds a new instance of field-insensitive point-based heap from its heap
	 * environment and substitutions.
	 * 
	 * @param heapEnv the heap environment that this instance tracks
	 */
	protected PointBasedHeap(HeapEnvironment<AllocationSites> heapEnv) {
		this.heapEnv = heapEnv;
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
			if (exp instanceof MemoryPointer) {
				MemoryPointer pid = (MemoryPointer) exp;
				Identifier v = id instanceof MemoryPointer ? ((MemoryPointer) id).getReferencedLocation() : id;
				HeapEnvironment<AllocationSites> heap = sss.heapEnv.assign(v, pid.getReferencedLocation(), pp);
				result = result.lub(from(new PointBasedHeap(heap)));
			} else
				result = result.lub(sss);
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
		return from(new PointBasedHeap(heapEnv.forgetIdentifier(id)));
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		// we leave the decision to the value domain
		return Satisfiability.UNKNOWN;
	}

	// @Override
	// public DomainRepresentation representation() {
	// if (isTop())
	// return Lattice.TOP_REPR;
	//
	// if (isBottom())
	// return Lattice.BOTTOM_REPR;
	//
	// return heapEnv.representation();
	// }

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
		return from(new PointBasedHeap(heapEnv.top()));
	}

	@Override
	public boolean isTop() {
		return heapEnv.isTop();
	}

	@Override
	public PointBasedHeap bottom() {
		return from(new PointBasedHeap(heapEnv.bottom()));
	}

	@Override
	public boolean isBottom() {
		return heapEnv.isBottom();
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return Collections.emptyList();
	}

	@Override
	public PointBasedHeap mk(PointBasedHeap reference) {
		return from(new PointBasedHeap(reference.heapEnv));
	}

	@Override
	protected PointBasedHeap lubAux(PointBasedHeap other) throws SemanticException {
		return from(new PointBasedHeap(heapEnv.lub(other.heapEnv)));
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
		return true;
	}

	@Override
	protected PointBasedHeap semanticsOf(HeapExpression expression, ProgramPoint pp) throws SemanticException {
		if (expression instanceof AccessChild) {
			AccessChild access = (AccessChild) expression;
			PointBasedHeap containerState = smallStepSemantics(access.getContainer(), pp);
			return containerState.smallStepSemantics(access.getChild(), pp);
		}

		if (expression instanceof HeapAllocation)
			return this;

		if (expression instanceof HeapReference)
			return smallStepSemantics(((HeapReference) expression).getExpression(), pp);

		if (expression instanceof HeapDereference)
			return smallStepSemantics(((HeapDereference) expression).getExpression(), pp);

		return top();
	}

	@Override
	public ExpressionSet<ValueExpression> rewrite(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return expression.accept(new Rewriter());
	}

	private Set<ValueExpression> resolveIdentifier(Identifier v) {
		Set<ValueExpression> result = new HashSet<>();
		for (AllocationSite site : heapEnv.getState(v))
			result.add(new MemoryPointer(site.getTypes(), site, site.getLocation()));

		return result;
	}

	@Override
	public PointBasedHeap popScope(ScopeToken scope) throws SemanticException {
		return from(new PointBasedHeap(heapEnv.popScope(scope)));
	}

	@Override
	public PointBasedHeap pushScope(ScopeToken scope) throws SemanticException {
		return from(new PointBasedHeap(heapEnv.pushScope(scope)));
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
				if (rec instanceof MemoryPointer) {
					MemoryPointer pid = (MemoryPointer) rec;
					AllocationSite site = (AllocationSite) pid.getReferencedLocation();
					result.add(new AllocationSite(expression.getTypes(), site.getId(), true, expression.getLocation()));
				}

			return new ExpressionSet<>(result);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapAllocation expression, Object... params)
				throws SemanticException {
			AllocationSite id = new AllocationSite(expression.getTypes(),
					expression.getLocation().getCodeLocation(), true, expression.getLocation());
			return new ExpressionSet<>(id);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapReference expression, ExpressionSet<ValueExpression> loc,
				Object... params)
				throws SemanticException {
			Set<ValueExpression> result = new HashSet<>();

			for (ValueExpression locExp : loc)
				if (locExp instanceof AllocationSite)
					result.add(new MemoryPointer(locExp.getTypes(), (AllocationSite) locExp, locExp.getLocation()));
				else
					result.add(locExp);
			return new ExpressionSet<>(result);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapDereference expression, ExpressionSet<ValueExpression> ref,
				Object... params)
				throws SemanticException {
			Set<ValueExpression> result = new HashSet<>();

			for (ValueExpression refExp : ref) {
				if (refExp instanceof Identifier && !(refExp instanceof MemoryPointer)) {
					Identifier id = (Identifier) refExp;
					if (heapEnv.getKeys().contains(id))
						result.addAll(resolveIdentifier(id));
				} else
					result.add(refExp);
			}

			return new ExpressionSet<>(result);
		}

		@Override
		public final ExpressionSet<ValueExpression> visit(Identifier expression, Object... params)
				throws SemanticException {
			if (!(expression instanceof MemoryPointer))
				if (heapEnv.getKeys().contains(expression))
					return new ExpressionSet<>(resolveIdentifier(expression));

			return new ExpressionSet<>(expression);
		}
	}
}