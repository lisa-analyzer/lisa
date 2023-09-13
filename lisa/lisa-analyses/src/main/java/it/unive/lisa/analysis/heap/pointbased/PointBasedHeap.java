package it.unive.lisa.analysis.heap.pointbased;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.BaseHeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.representation.StructuredRepresentation;

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
public class PointBasedHeap implements BaseHeapDomain<PointBasedHeap> {

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
		ExpressionSet rhsExprs = sss.rewrite(expression, pp);

		PointBasedHeap result = bottom();
		List<HeapReplacement> replacements = new LinkedList<>();
		for (SymbolicExpression rhs : rhsExprs)
			if (rhs instanceof MemoryPointer) {
				HeapLocation rhs_ref = ((MemoryPointer) rhs).getReferencedLocation();
				if (id instanceof MemoryPointer) {
					// we have x = y, where both are pointers
					// we perform *x = *y so that x and y
					// become aliases
					Identifier lhs_ref = ((MemoryPointer) id).getReferencedLocation();
					HeapEnvironment<AllocationSites> heap = sss.heapEnv.assign(lhs_ref, rhs_ref, pp);
					result = result.lub(from(new PointBasedHeap(heap)));
				} else {
					if (rhs_ref instanceof StackAllocationSite
							&& !getAlreadyAllocated(((StackAllocationSite) rhs_ref).getLocationName()).isEmpty())
						result = result
								.lub(nonAliasedAssignment(id, (StackAllocationSite) rhs_ref, sss, pp, replacements));
					else {
						// aliasing: id and star_y points to the same object
						HeapEnvironment<AllocationSites> heap = sss.heapEnv.assign(id, rhs_ref, pp);
						result = result.lub(from(new PointBasedHeap(heap)));
					}
				}
			} else
				result = result.lub(sss);

		return buildHeapAfterAssignment(result, replacements);
	}

	/**
	 * Yields an allocation site name {@code id} if it is tracked by this
	 * domain, {@code null} otherwise.
	 * 
	 * @param location allocation site's name to be searched
	 * 
	 * @return an allocation site name {@code id} if it is tracked by this
	 *             domain, {@code null} otherwise
	 */
	protected Set<Pair<Identifier, AllocationSite>> getAlreadyAllocated(String location) {
		Set<Pair<Identifier, AllocationSite>> sites = new HashSet<>();
		for (Entry<Identifier, AllocationSites> set : heapEnv)
			for (AllocationSite site : set.getValue())
				if (site.getLocationName().equals(location))
					sites.add(Pair.of(set.getKey(), site));

		return sites;
	}

	/**
	 * Builds an instance of the heap domain after an assignment, from the heap
	 * instance obtained from the small step semantics of the right-hand side of
	 * the assignment and the replacements.
	 * 
	 * @param sss          the point-based heap instance obtained from the small
	 *                         step semantics of the right-hand side of the
	 *                         assignment
	 * @param replacements the list of replacements to be updated
	 * 
	 * @return an instance of the point-based heap domain after an assignment,
	 *             from the resulting heap domain, the heap instance obtained
	 *             from the small step semantics of the right-hand side of the
	 *             assignment, and the replacements
	 */
	protected PointBasedHeap buildHeapAfterAssignment(PointBasedHeap sss,
			List<HeapReplacement> replacements) {
		return from(new PointBasedHeap(sss.heapEnv, replacements));
	}

	/**
	 * Given the point-based heap instance {@code pb}, perform the assignment of
	 * {@code site} to the identifier {@code id} when {@code site} is a static
	 * allocation site, thus handling the heap replacements.
	 * 
	 * @param id           the identifier to be updated
	 * @param site         the allocation site to be assigned
	 * @param pb           the starting point-based heap instance
	 * @param pp           the program point where this operation occurs
	 * @param replacements the list of replacements to be updated
	 * 
	 * @return the point-based heap instance where {@code id} is updated with
	 *             {@code star_y} and the needed heap replacements
	 * 
	 * @throws SemanticException if something goes wrong during the analysis
	 */
	public PointBasedHeap nonAliasedAssignment(Identifier id, StackAllocationSite site, PointBasedHeap pb,
			ProgramPoint pp, List<HeapReplacement> replacements)
			throws SemanticException {
		// no aliasing: star_y must be cloned and the clone must
		// be assigned to id
		StackAllocationSite clone = new StackAllocationSite(site.getStaticType(),
				id.getCodeLocation().toString(), site.isWeak(), id.getCodeLocation());
		// also runtime types are inherited, if already inferred
		if (site.hasRuntimeTypes())
			clone.setRuntimeTypes(site.getRuntimeTypes(null));

		HeapEnvironment<AllocationSites> tmp = pb.heapEnv.assign(id, clone, pp);

		HeapReplacement replacement = new HeapReplacement();
		replacement.addSource(site);
		replacement.addTarget(clone);
		replacement.addTarget(site);
		replacements.add(replacement);

		return from(new PointBasedHeap(tmp));
	}

	@Override
	public PointBasedHeap assume(SymbolicExpression expression, ProgramPoint src, ProgramPoint dest)
			throws SemanticException {
		// we just rewrite the expression if needed
		return smallStepSemantics(expression, src);
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
	public StructuredRepresentation representation() {
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
	public ExpressionSet rewrite(SymbolicExpression expression, ProgramPoint pp)
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
		public ExpressionSet visit(AccessChild expression, ExpressionSet receiver,
				ExpressionSet child, Object... params) throws SemanticException {
			Set<SymbolicExpression> result = new HashSet<>();
			for (SymbolicExpression rec : receiver)
				if (rec instanceof MemoryPointer) {
					MemoryPointer pid = (MemoryPointer) rec;
					AllocationSite site = (AllocationSite) pid.getReferencedLocation();
					AllocationSite e;
					if (site instanceof StackAllocationSite)
						e = new StackAllocationSite(
								expression.getStaticType(),
								site.getLocationName(),
								true,
								expression.getCodeLocation());
					else
						e = new HeapAllocationSite(
								expression.getStaticType(),
								site.getLocationName(),
								true,
								expression.getCodeLocation());

					Set<Type> types = new HashSet<>();
					if (expression.hasRuntimeTypes())
						types.addAll(expression.getRuntimeTypes(null));

					if (rec.hasRuntimeTypes())
						types.addAll(rec.getRuntimeTypes(null));

					if (!types.isEmpty())
						e.setRuntimeTypes(types);

					result.add(e);
				} else if (rec instanceof AllocationSite)
					result.add(rec);

			return new ExpressionSet(result);
		}

		@Override
		public ExpressionSet visit(MemoryAllocation expression, Object... params)
				throws SemanticException {
			AllocationSite id;
			if (expression.isStackAllocation())
				id = new StackAllocationSite(
						expression.getStaticType(),
						expression.getCodeLocation().getCodeLocation(),
						true,
						expression.getCodeLocation());
			else
				id = new HeapAllocationSite(
						expression.getStaticType(),
						expression.getCodeLocation().getCodeLocation(),
						true,
						expression.getCodeLocation());

			if (expression.hasRuntimeTypes())
				id.setRuntimeTypes(expression.getRuntimeTypes(null));
			return new ExpressionSet(id);
		}

		@Override
		public ExpressionSet visit(HeapReference expression, ExpressionSet arg,
				Object... params)
				throws SemanticException {
			Set<SymbolicExpression> result = new HashSet<>();

			for (SymbolicExpression loc : arg)
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
			return new ExpressionSet(result);
		}

		@Override
		public ExpressionSet visit(HeapDereference expression, ExpressionSet arg,
				Object... params)
				throws SemanticException {
			Set<SymbolicExpression> result = new HashSet<>();

			for (SymbolicExpression ref : arg)
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
						AllocationSite site;
						if (id.getStaticType().isPointerType())
							site = new HeapAllocationSite(id.getStaticType(), "unknown@" + id.getName(), true, loc);
						else if (id.getStaticType().isInMemoryType() || id.getStaticType().isUntyped())
							site = new StackAllocationSite(id.getStaticType(), "unknown@" + id.getName(), true, loc);
						else
							throw new SemanticException("The type " + id.getStaticType()
									+ " cannot be allocated by point-based heap domains");
						result.add(site);
					}
				} else
					result.add(ref);

			return new ExpressionSet(result);
		}

		@Override
		public ExpressionSet visit(Identifier expression, Object... params)
				throws SemanticException {
			if (!(expression instanceof MemoryPointer) && heapEnv.getKeys().contains(expression))
				return new ExpressionSet(resolveIdentifier(expression));

			return new ExpressionSet(expression);
		}

		private Set<SymbolicExpression> resolveIdentifier(Identifier v) {
			Set<SymbolicExpression> result = new HashSet<>();
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
		public ExpressionSet visit(PushAny expression, Object... params)
				throws SemanticException {
			if (expression.getStaticType().isPointerType()) {
				Type inner = expression.getStaticType().asPointerType().getInnerType();
				CodeLocation loc = expression.getCodeLocation();
				HeapAllocationSite site = new HeapAllocationSite(inner, "unknown@" + loc.getCodeLocation(), false, loc);
				return new ExpressionSet(new MemoryPointer(expression.getStaticType(), site, loc));
			} else if (expression.getStaticType().isInMemoryType()) {
				Type type = expression.getStaticType();
				CodeLocation loc = expression.getCodeLocation();
				StackAllocationSite site = new StackAllocationSite(type, "unknown@" + loc.getCodeLocation(), false,
						loc);
				return new ExpressionSet(new MemoryPointer(expression.getStaticType(), site, loc));
			}
			return new ExpressionSet(expression);
		}
	}
}
