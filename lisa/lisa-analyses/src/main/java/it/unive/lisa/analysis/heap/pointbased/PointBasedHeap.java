package it.unive.lisa.analysis.heap.pointbased;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.BaseHeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.program.cfg.CodeLocation;
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
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

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
	public final HeapEnvironment<AllocationSites> heapEnv;

	private final List<HeapReplacement> replacements;

	/**
	 * Builds a new instance of field-insensitive point-based heap.
	 */
	public PointBasedHeap() {
		this(new HeapEnvironment<>(new AllocationSites()));
	}

	/**
	 * Builds a new instance of field-insensitive point-based heap from its heap
	 * environment.
	 * 
	 * @param heapEnv the heap environment that this instance tracks
	 */
	public PointBasedHeap(HeapEnvironment<AllocationSites> heapEnv) {
		this(heapEnv, Collections.emptyList());
	}

	/**
	 * Builds a new instance of field-insensitive point-based heap from its heap
	 * environment.
	 * 
	 * @param heapEnv      the heap environment that this instance tracks
	 * @param replacements the heap replacements of this instance
	 */
	public PointBasedHeap(HeapEnvironment<AllocationSites> heapEnv, List<HeapReplacement> replacements) {
		this.heapEnv = heapEnv;
		this.replacements = replacements.isEmpty() ? Collections.emptyList() : replacements;
	}

	/**
	 * Builds a point-based heap from a reference one.
	 * 
	 * @param original reference point-based heap
	 * 
	 * @return a point-based heap build from the original one
	 */
	public PointBasedHeap from(PointBasedHeap original) {
		return original;
	}

	@Override
	public PointBasedHeap assign(Identifier id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {

		PointBasedHeap sss = smallStepSemantics(expression, pp);
		ExpressionSet<ValueExpression> rewrittenExp = sss.rewrite(expression, pp);

		PointBasedHeap result = bottom();
		for (ValueExpression exp : rewrittenExp)
			if (exp instanceof MemoryPointer) {
				MemoryPointer pid = (MemoryPointer) exp;
				HeapLocation star_y = pid.getReferencedLocation();
				if (id instanceof MemoryPointer) {
					// we have x = y, where both are pointers
					// we perform *x = *y so that x and y
					// become aliases
					Identifier star_x = ((MemoryPointer) id).getReferencedLocation();
					HeapEnvironment<AllocationSites> heap = sss.heapEnv.assign(star_x, star_y, pp);
					result = result.lub(from(new PointBasedHeap(heap)));
				} else {
					if (star_y instanceof StaticAllocationSite)
						result = result.lub(staticAllocation(id, (StaticAllocationSite) star_y, sss, pp));
					else {
						// aliasing: id and star_y points to the same object
						HeapEnvironment<AllocationSites> heap = sss.heapEnv.assign(id, star_y, pp);
						result = result.lub(from(new PointBasedHeap(heap)));
					}
				}
			} else
				result = result.lub(sss);

		return result;
	}

	/**
	 * Given the point-based heap instance {@code sss}, perform the assignment
	 * of {@code star_y} to the identifier {@code id} when {@code star_y} is a
	 * static allocation site, thus handling the heap replacements.
	 * 
	 * @param id     the identifier to be updated
	 * @param star_y the allocation site to be assigned
	 * @param sss    the starting point-based heap instance
	 * @param pp     the program point where this operation occurs
	 * 
	 * @return the point-based heap instace where {@code id} is updated with
	 *             {@code star_y} and the needed heap replacements
	 * 
	 * @throws SemanticException
	 */
	protected PointBasedHeap staticAllocation(Identifier id, StaticAllocationSite star_y, PointBasedHeap sss,
			ProgramPoint pp)
			throws SemanticException {
		// no aliasing: star_y must be cloned and the clone must
		// be assigned to id
		StaticAllocationSite clone = new StaticAllocationSite(star_y.getStaticType(),
				id.getCodeLocation().toString(), star_y.isWeak(), id.getCodeLocation());
		// also runtime types are inherited, if already inferred
		if (star_y.hasRuntimeTypes())
			clone.setRuntimeTypes(star_y.getRuntimeTypes(null));

		HeapEnvironment<AllocationSites> tmp = sss.heapEnv.assign(id, clone, pp);

		HeapReplacement replacement = new HeapReplacement();
		replacement.addSource(star_y);
		replacement.addTarget(clone);
		replacement.addTarget(star_y);
		return from(new PointBasedHeap(tmp, new ArrayList<>(Collections.singleton(replacement))));
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
	public PointBasedHeap forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
		return from(new PointBasedHeap(heapEnv.forgetIdentifiersIf(test)));
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		// we leave the decision to the value domain
		return Satisfiability.UNKNOWN;
	}

	@Override
	public DomainRepresentation representation() {
		if (isTop())
			return Lattice.topRepresentation();

		if (isBottom())
			return Lattice.bottomRepresentation();

		return heapEnv.representation();
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
		return replacements;
	}

	@Override
	public PointBasedHeap mk(PointBasedHeap reference) {
		return from(new PointBasedHeap(reference.heapEnv));
	}

	@Override
	public PointBasedHeap lubAux(PointBasedHeap other) throws SemanticException {
		return from(new PointBasedHeap(heapEnv.lub(other.heapEnv)));
	}

	@Override
	public PointBasedHeap glbAux(PointBasedHeap other) throws SemanticException {
		return from(new PointBasedHeap(heapEnv.glb(other.heapEnv)));
	}

	@Override
	public boolean lessOrEqualAux(PointBasedHeap other) throws SemanticException {
		return heapEnv.lessOrEqual(other.heapEnv);
	}

	@Override
	public int hashCode() {
		return Objects.hash(heapEnv, replacements);
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
		return Objects.equals(heapEnv, other.heapEnv) && Objects.equals(replacements, other.replacements);
	}

	@Override
	public PointBasedHeap semanticsOf(HeapExpression expression, ProgramPoint pp) throws SemanticException {
		return this;
	}

	@Override
	public ExpressionSet<ValueExpression> rewrite(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return expression.accept(new Rewriter());
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
	public class Rewriter extends BaseHeapDomain.Rewriter {

		/*
		 * note that all the cases where we are adding a plain expression to the
		 * result set in these methods is because it could have been already
		 * rewritten by other rewrite methods to an allocation site
		 */

		@Override
		public ExpressionSet<ValueExpression> visit(AccessChild expression, ExpressionSet<ValueExpression> receiver,
				ExpressionSet<ValueExpression> child, Object... params) throws SemanticException {
			Set<ValueExpression> result = new HashSet<>();

			for (ValueExpression rec : receiver)
				if (rec instanceof MemoryPointer) {
					MemoryPointer pid = (MemoryPointer) rec;
					AllocationSite site = (AllocationSite) pid.getReferencedLocation();
					AllocationSite e;
					if (site instanceof StaticAllocationSite)
						e = new StaticAllocationSite(
								expression.getStaticType(),
								site.getLocationName(),
								true,
								expression.getCodeLocation());
					else
						e = new DynamicAllocationSite(
								expression.getStaticType(),
								site.getLocationName(),
								true,
								expression.getCodeLocation());

					if (expression.hasRuntimeTypes())
						e.setRuntimeTypes(expression.getRuntimeTypes(null));
					result.add(e);
				} else if (rec instanceof AllocationSite)
					result.add(rec);

			return new ExpressionSet<>(result);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapAllocation expression, Object... params)
				throws SemanticException {
			AllocationSite id;
			if (expression.isStaticallyAllocated())
				id = new StaticAllocationSite(
						expression.getStaticType(),
						expression.getCodeLocation().getCodeLocation(),
						true,
						expression.getCodeLocation());
			else
				id = new DynamicAllocationSite(
						expression.getStaticType(),
						expression.getCodeLocation().getCodeLocation(),
						true,
						expression.getCodeLocation());

			if (expression.hasRuntimeTypes())
				id.setRuntimeTypes(expression.getRuntimeTypes(null));
			return new ExpressionSet<>(id);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapReference expression, ExpressionSet<ValueExpression> arg,
				Object... params)
				throws SemanticException {
			Set<ValueExpression> result = new HashSet<>();

			for (ValueExpression loc : arg)
				if (loc instanceof AllocationSite) {
					MemoryPointer e = new MemoryPointer(
							new ReferenceType(loc.getStaticType()),
							(AllocationSite) loc,
							loc.getCodeLocation());
					if (expression.hasRuntimeTypes())
						e.setRuntimeTypes(expression.getRuntimeTypes(null));
					result.add(e);
				} else
					result.add(loc);
			return new ExpressionSet<>(result);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapDereference expression, ExpressionSet<ValueExpression> arg,
				Object... params)
				throws SemanticException {
			Set<ValueExpression> result = new HashSet<>();

			for (ValueExpression ref : arg)
				if (ref instanceof MemoryPointer)
					result.add(((MemoryPointer) ref).getReferencedLocation());
				else if (ref instanceof Identifier) {
					// this could be aliasing!
					Identifier id = (Identifier) ref;
					if (heapEnv.getKeys().contains(id))
						result.addAll(resolveIdentifier(id));
					else if (id instanceof Variable) {
						// this is a variable from the program that we know
						// nothing about
						CodeLocation loc = expression.getCodeLocation();
						AllocationSite site = new AllocationSite(id.getStaticType(), "unknown@" + id.getName(), loc);
						result.add(site);
					}
				} else
					result.add(ref);

			return new ExpressionSet<>(result);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(Identifier expression, Object... params)
				throws SemanticException {
			if (!(expression instanceof MemoryPointer) && heapEnv.getKeys().contains(expression))
				return new ExpressionSet<>(resolveIdentifier(expression));

			return new ExpressionSet<>(expression);
		}

		private Set<ValueExpression> resolveIdentifier(Identifier v) {
			Set<ValueExpression> result = new HashSet<>();
			for (AllocationSite site : heapEnv.getState(v)) {
				MemoryPointer e = new MemoryPointer(
						new ReferenceType(site.getStaticType()),
						site,
						site.getCodeLocation());
				if (v.hasRuntimeTypes())
					e.setRuntimeTypes(v.getRuntimeTypes(null));
				result.add(e);
			}

			return result;
		}

		@Override
		public ExpressionSet<ValueExpression> visit(PushAny expression, Object... params)
				throws SemanticException {
			if (expression.getStaticType().isPointerType()) {
				Type inner = expression.getStaticType().asPointerType().getInnerType();
				CodeLocation loc = expression.getCodeLocation();
				AllocationSite site = new AllocationSite(inner, "unknown@" + loc.getCodeLocation(), loc);
				return new ExpressionSet<>(new MemoryPointer(expression.getStaticType(), site, loc));
			}
			return new ExpressionSet<>(expression);
		}
	}
}
