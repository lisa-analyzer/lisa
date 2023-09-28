package it.unive.lisa.analysis.heap.pointbased;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.heap.BaseHeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.program.annotations.Annotation;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A base class for heap analyses based on the allocation sites of the objects
 * and arrays they track, namely the position of the code where heap locations
 * are generated. All heap locations that are generated at the same allocation
 * sites are abstracted into a single unique heap identifier. Concrete instances
 * have control over their field-sensitivity.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the concrete type of analysis that methods of this class return
 */
public abstract class AllocationSiteBasedAnalysis<A extends AllocationSiteBasedAnalysis<A>>
		implements
		BaseHeapDomain<A> {

	/**
	 * An heap environment tracking which allocation sites are associated to
	 * each identifier.
	 */
	public final HeapEnvironment<AllocationSites> heapEnv;

	/**
	 * The replacements to be applied after the generation of this domain
	 * instance.
	 */
	public final List<HeapReplacement> replacements;

	/**
	 * Builds a new instance of allocation site-based heap.
	 */
	protected AllocationSiteBasedAnalysis() {
		this(new HeapEnvironment<>(new AllocationSites()));
	}

	/**
	 * Builds a new instance of allocation site-based heap from its heap
	 * environment.
	 * 
	 * @param heapEnv the heap environment that this instance tracks
	 */
	public AllocationSiteBasedAnalysis(
			HeapEnvironment<AllocationSites> heapEnv) {
		this(heapEnv, Collections.emptyList());
	}

	/**
	 * Builds a new instance of allocation site-based heap from its heap
	 * environment and replacements.
	 * 
	 * @param heapEnv      the heap environment that this instance tracks
	 * @param replacements the heap replacements of this instance
	 */
	public AllocationSiteBasedAnalysis(
			HeapEnvironment<AllocationSites> heapEnv,
			List<HeapReplacement> replacements) {
		this.heapEnv = heapEnv;
		this.replacements = replacements.isEmpty() ? Collections.emptyList() : replacements;
	}

	@Override
	public A mk(
			A reference) {
		return mk(reference.heapEnv, Collections.emptyList());
	}

	/**
	 * Builds a new instance of this class given its components.
	 * 
	 * @param heapEnv the heap environment that this instance tracks
	 * 
	 * @return the new instance
	 */
	protected A mk(
			HeapEnvironment<AllocationSites> heapEnv) {
		return mk(heapEnv, Collections.emptyList());
	}

	/**
	 * Builds a new instance of this class given its components.
	 * 
	 * @param heapEnv      the heap environment that this instance tracks
	 * @param replacements the heap replacements of this instance
	 * 
	 * @return the new instance
	 */
	protected abstract A mk(
			HeapEnvironment<AllocationSites> heapEnv,
			List<HeapReplacement> replacements);

	@Override
	public A assign(
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		A sss = smallStepSemantics(expression, pp, oracle);
		A result = bottom();
		List<HeapReplacement> replacements = new LinkedList<>();
		ExpressionSet rhsExps;
		if (expression.mightNeedRewriting())
			rhsExps = rewrite(expression, pp, oracle);
		else
			rhsExps = new ExpressionSet(expression);

		for (SymbolicExpression rhs : rhsExps)
			if (rhs instanceof MemoryPointer) {
				HeapLocation rhs_ref = ((MemoryPointer) rhs).getReferencedLocation();
				if (id instanceof MemoryPointer) {
					// we have x = y, where both are pointers we perform *x = *y
					// so that x and y become aliases
					Identifier lhs_ref = ((MemoryPointer) id).getReferencedLocation();
					HeapEnvironment<AllocationSites> heap = sss.heapEnv.assign(lhs_ref, rhs_ref, pp, oracle);
					result = result.lub(mk(heap));
				} else if (rhs_ref instanceof StackAllocationSite
						&& !getAllocatedAt(((StackAllocationSite) rhs_ref).getLocationName()).isEmpty())
					// for stack elements, assignment works as a shallow copy
					// since there are no pointers to alias
					result = result.lub(sss.shallowCopy(id, (StackAllocationSite) rhs_ref, replacements, pp, oracle));
				else {
					// aliasing: id and star_y points to the same object
					HeapEnvironment<AllocationSites> heap = sss.heapEnv.assign(id, rhs_ref, pp, oracle);
					result = result.lub(mk(heap));
				}
			} else
				result = result.lub(sss);

		return mk(result.heapEnv, replacements);
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
	protected Set<AllocationSite> getAllocatedAt(
			String location) {
		Set<AllocationSite> sites = new HashSet<>();
		for (AllocationSites set : heapEnv.getValues())
			for (AllocationSite site : set)
				if (site.getLocationName().equals(location))
					sites.add(site);

		return sites;
	}

	/**
	 * Performs the assignment of {@code site} to the identifier {@code id} when
	 * {@code site} is a static allocation site, thus performing a shallow copy
	 * instead of aliasing handling the heap replacements.
	 * 
	 * @param id           the identifier to be updated
	 * @param site         the allocation site to be assigned
	 * @param replacements the list of replacements to be updated
	 * @param pp           the program point where this operation occurs
	 * @param oracle       the oracle for inter-domain communication
	 * 
	 * @return the point-based heap instance where {@code id} is updated with
	 *             {@code star_y} and the needed heap replacements
	 * 
	 * @throws SemanticException if something goes wrong during the analysis
	 */
	public A shallowCopy(
			Identifier id,
			StackAllocationSite site,
			List<HeapReplacement> replacements,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// no aliasing: star_y must be cloned and the clone must
		// be assigned to id
		StackAllocationSite clone = new StackAllocationSite(site.getStaticType(),
				id.getCodeLocation().toString(), site.isWeak(), id.getCodeLocation());
		HeapEnvironment<AllocationSites> tmp = heapEnv.assign(id, clone, pp, oracle);

		HeapReplacement replacement = new HeapReplacement();
		replacement.addSource(site);
		replacement.addTarget(clone);
		replacement.addTarget(site);
		replacements.add(replacement);

		return mk(tmp);
	}

	@Override
	public A assume(
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		// we just rewrite the expression if needed
		return smallStepSemantics(expression, src, oracle);
	}

	@Override
	public Satisfiability satisfies(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
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
	public boolean isTop() {
		return heapEnv.isTop();
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
	public boolean lessOrEqualAux(
			A other)
			throws SemanticException {
		return heapEnv.lessOrEqual(other.heapEnv);
	}

	@Override
	public int hashCode() {
		return Objects.hash(heapEnv, replacements);
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
		@SuppressWarnings("unchecked")
		A other = (A) obj;
		return Objects.equals(heapEnv, other.heapEnv) && Objects.equals(replacements, other.replacements);
	}

	@Override
	@SuppressWarnings("unchecked")
	public A semanticsOf(
			HeapExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return (A) this;
	}

	@Override
	public ExpressionSet rewrite(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return expression.accept(new Rewriter(), pp, oracle);
	}

	/**
	 * A {@link it.unive.lisa.analysis.heap.BaseHeapDomain.Rewriter} for the
	 * {@link AllocationSiteBasedAnalysis} domain.
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
		public ExpressionSet visit(
				AccessChild expression,
				ExpressionSet receiver,
				ExpressionSet child,
				Object... params)
				throws SemanticException {
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

					// propagates the annotations of the child value expression
					// to the newly created allocation site
					for (SymbolicExpression f : child)
						if (f instanceof Identifier)
							for (Annotation ann : e.getAnnotations())
								e.addAnnotation(ann);

					result.add(e);
				} else if (rec instanceof AllocationSite)
					result.add(rec);

			return new ExpressionSet(result);
		}

		@Override
		public ExpressionSet visit(
				MemoryAllocation expression,
				Object... params)
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

			// propagates the annotations of expression
			// to the newly created allocation site
			for (Annotation ann : expression.getAnnotations())
				id.addAnnotation(ann);

			return new ExpressionSet(id);
		}

		@Override
		public ExpressionSet visit(
				HeapReference expression,
				ExpressionSet arg,
				Object... params)
				throws SemanticException {
			Set<SymbolicExpression> result = new HashSet<>();

			for (SymbolicExpression loc : arg)
				if (loc instanceof AllocationSite) {
					AllocationSite allocSite = (AllocationSite) loc;
					MemoryPointer e = new MemoryPointer(
							new ReferenceType(loc.getStaticType()),
							allocSite,
							loc.getCodeLocation());

					// propagates the annotations of the allocation site
					// to the newly created memory pointer
					for (Annotation ann : allocSite.getAnnotations())
						e.addAnnotation(ann);

					result.add(e);
				} else
					result.add(loc);
			return new ExpressionSet(result);
		}

		@Override
		public ExpressionSet visit(
				HeapDereference expression,
				ExpressionSet arg,
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

						// propagates the annotations of the variable
						// to the newly created allocation site
						for (Annotation ann : id.getAnnotations())
							site.addAnnotation(ann);

						result.add(site);
					}
				} else
					result.add(ref);

			return new ExpressionSet(result);
		}

		@Override
		public ExpressionSet visit(
				Identifier expression,
				Object... params)
				throws SemanticException {
			if (!(expression instanceof MemoryPointer) && heapEnv.getKeys().contains(expression))
				return new ExpressionSet(resolveIdentifier(expression));

			return new ExpressionSet(expression);
		}

		private Set<SymbolicExpression> resolveIdentifier(
				Identifier v) {
			Set<SymbolicExpression> result = new HashSet<>();
			for (AllocationSite site : heapEnv.getState(v)) {
				MemoryPointer e = new MemoryPointer(
						new ReferenceType(site.getStaticType()),
						site,
						site.getCodeLocation());
				result.add(e);
			}

			return result;
		}

		@Override
		public ExpressionSet visit(
				PushAny expression,
				Object... params)
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

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return heapEnv.knowsIdentifier(id) || (id instanceof AllocationSite
				&& heapEnv.getValues().stream().anyMatch(as -> as.contains((AllocationSite) id)));
	}
}
