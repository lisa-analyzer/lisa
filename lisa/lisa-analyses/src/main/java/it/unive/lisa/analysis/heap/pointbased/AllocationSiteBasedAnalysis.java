package it.unive.lisa.analysis.heap.pointbased;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.heap.BaseHeapDomain;
import it.unive.lisa.analysis.heap.HeapLattice;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.lattices.Satisfiability;
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
import it.unive.lisa.util.collections.workset.VisitOnceFIFOWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A base class for heap analyses based on the allocation sites of the objects
 * and arrays they track, namely the position of the code where heap locations
 * are generated. All heap locations that are generated at the same allocation
 * sites are abstracted into a single unique heap identifier. Concrete instances
 * have control over their field-sensitivity.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type {@link FunctionalLattice} used to track points-to
 *                information for the heap locations
 */
public abstract class AllocationSiteBasedAnalysis<
		L extends FunctionalLattice<L, Identifier, AllocationSites> & HeapLattice<L>>
		implements
		BaseHeapDomain<L> {

	private final Rewriter rewriter = new Rewriter();

	@Override
	public Pair<L, List<HeapReplacement>> assign(
			L state,
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Pair<L, List<HeapReplacement>> sss = smallStepSemantics(state, expression, pp, oracle);
		L result = state.bottom();
		List<HeapReplacement> replacements = new LinkedList<>();
		sss.getRight().forEach(replacements::add);
		ExpressionSet rhsExps;
		boolean rhsIsReceiver = false;

		expression = expression.removeTypingExpressions();

		if (expression instanceof Identifier) {
			rhsExps = new ExpressionSet(resolveIdentifier(state, (Identifier) expression));
			rhsIsReceiver = ((Identifier) expression).isInstrumentedReceiver();
		} else if (expression.mightNeedRewriting())
			rhsExps = rewrite(state, expression, pp, oracle);
		else
			rhsExps = new ExpressionSet(expression);

		for (SymbolicExpression rhs : rhsExps)
			result = result.lub(process(id, pp, oracle, sss.getLeft(), replacements, rhs, rhsIsReceiver));

		if (!id.isWeak() && state.knowsIdentifier(id)) {
			// we might make some location unreachable,
			// so we have to perform garbage collection
			HeapReplacement r = new HeapReplacement();
			r.addSource(id);
			replacements.addAll(state.expand(r));
		}

		return Pair.of(result, replacements);
	}

	private L process(
			Identifier id,
			ProgramPoint pp,
			SemanticOracle oracle,
			L sss,
			List<HeapReplacement> replacements,
			SymbolicExpression rhs,
			boolean rhsIsReceiver)
			throws SemanticException {
		if (rhs instanceof MemoryPointer) {
			if (!(((MemoryPointer) rhs).getReferencedLocation() instanceof AllocationSite))
				throw new SemanticException("Cannot assign a non-allocation site location");
			// we have x = y, where y is a pointer to an allocation site
			AllocationSite rhs_ref = (AllocationSite) ((MemoryPointer) rhs).getReferencedLocation();
			if (id instanceof MemoryPointer) {
				// we have x = y, where both are pointers
				// we perform *x = *y so that x and y become aliases
				Identifier lhs_ref = ((MemoryPointer) id).getReferencedLocation();
				return store(sss, lhs_ref, rhs_ref);
			} else if (rhs_ref instanceof StackAllocationSite
					// if we are allocating, we just perform normal aliasing
					// as there is nothing to copy
					&& !((StackAllocationSite) rhs_ref).isAllocation()
					// if rhs is an instrumented receiver, it corresponds to
					// something that is still on the stack while being
					// initialized (eg with a constructor call) so we
					// perform normal aliasing as there is nothing to copy
					&& !rhsIsReceiver
					&& !getAllocatedAt(sss, ((StackAllocationSite) rhs_ref).getLocationName()).isEmpty())
				// for stack elements, assignment works as a shallow copy
				// since there are no pointers to alias
				return shallowCopy(sss, id, (StackAllocationSite) rhs_ref, replacements);
			else {
				// aliasing: id and star_y points to the same object
				return store(sss, id, rhs_ref);
			}
		} else
			return sss;
	}

	/**
	 * Stores the allocation site {@code site} in the identifier {@code id} in
	 * the given state. This method ensures that (i) weak identifiers are
	 * correctly handled (performing a join with the current state of the
	 * identifier), and that the site being stored does not correspond to an
	 * allocation according to {@link HeapLocation#isAllocation()}.
	 * 
	 * @param state the state where to store the allocation site
	 * @param id    the identifier where to store the allocation site
	 * @param site  the allocation site to be stored
	 * 
	 * @return a new state where {@code id} is updated with {@code site}
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	protected L store(
			L state,
			Identifier id,
			AllocationSite site)
			throws SemanticException {
		if (site.isAllocation())
			// we never store the allocation version of a site,
			// otherwise it would be considered an allocation
			// every time we retrieve it from the map
			site = (AllocationSite) site.asNonAllocation();
		AllocationSites states = new AllocationSites(site);
		if (id.isWeak() && state.knowsIdentifier(id))
			states = states.lub(state.getState(id));
		return state.putState(id, states);
	}

	/**
	 * Yields an allocation site name {@code id} if it is tracked by this
	 * domain, {@code null} otherwise.
	 * 
	 * @param state    the current state of the analysis
	 * @param location allocation site's name to be searched
	 * 
	 * @return an allocation site name {@code id} if it is tracked by this
	 *             domain, {@code null} otherwise
	 */
	protected Set<AllocationSite> getAllocatedAt(
			L state,
			String location) {
		Set<AllocationSite> sites = new HashSet<>();
		for (AllocationSites set : state.getValues())
			for (AllocationSite site : set)
				if (site.getLocationName().equals(location))
					sites.add(site);

		return sites;
	}

	/**
	 * Performs the assignment of {@code site} to the identifier {@code id} when
	 * {@code site} is a stack allocation site, thus performing a shallow copy
	 * instead of aliasing handling the heap replacements.
	 * 
	 * @param state        the current state of the analysis
	 * @param id           the identifier to be updated
	 * @param site         the allocation site to be assigned
	 * @param replacements the list of replacements to be updated
	 * 
	 * @return the point-based heap instance where {@code id} is updated with
	 *             {@code star_y} and the needed heap replacements
	 * 
	 * @throws SemanticException if something goes wrong during the analysis
	 */
	public L shallowCopy(
			L state,
			Identifier id,
			StackAllocationSite site,
			List<HeapReplacement> replacements)
			throws SemanticException {
		// no aliasing: star_y must be cloned and the clone must
		// be assigned to id
		StackAllocationSite clone = new StackAllocationSite(
				site.getStaticType(),
				id.getCodeLocation().toString(),
				site.isWeak(),
				id.getCodeLocation());

		HeapReplacement replacement = new HeapReplacement();
		replacement.addSource(site);
		replacement.addTarget(clone);
		replacement.addTarget(site);
		replacements.add(replacement);

		return store(state, id, clone);
	}

	@Override
	public Pair<L, List<HeapReplacement>> assume(
			L state,
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return Pair.of(state, List.of());
	}

	@Override
	public Pair<L, List<HeapReplacement>> semanticsOf(
			L state,
			HeapExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Pair.of(state, List.of());
	}

	@Override
	public ExpressionSet rewrite(
			L state,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return expression.accept(rewriter, state);
	}

	/**
	 * Resolves the identifier {@code v} to the set of allocation sites that it
	 * points to, if it is not a memory pointer and this domain instance
	 * contains points-to information for {@code v}. Otherwise, a set with the
	 * given identifier is returned.
	 * 
	 * @param state the current state of the analysis
	 * @param v     the identifier to be resolved
	 * 
	 * @return the set of allocation sites that {@code v} points to, or a set
	 *             with {@code v} itself
	 */
	protected Set<SymbolicExpression> resolveIdentifier(
			L state,
			Identifier v) {
		if (v instanceof MemoryPointer || !state.getKeys().contains(v))
			return Set.of(v);

		Set<SymbolicExpression> result = new HashSet<>();
		for (AllocationSite site : state.getState(v))
			result.add(new MemoryPointer(new ReferenceType(site.getStaticType()), site, site.getCodeLocation()));

		return result;
	}

	/**
	 * A {@link it.unive.lisa.analysis.heap.BaseHeapDomain.Rewriter} for the
	 * {@link AllocationSiteBasedAnalysis} domain.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public class Rewriter
			extends
			BaseHeapDomain.Rewriter {

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
			@SuppressWarnings("unchecked")
			L state = (L) params[0];

			Set<SymbolicExpression> toProcess = new HashSet<>();
			for (SymbolicExpression rec : receiver) {
				rec = rec.removeTypingExpressions();
				if (rec instanceof Identifier)
					toProcess.addAll(resolveIdentifier(state, (Identifier) rec));
				else
					toProcess.add(rec);
			}

			for (SymbolicExpression rec : toProcess)
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
					result.add(((AllocationSite) rec).withType(expression.getStaticType()));

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
			id.setAllocation(true);

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
			@SuppressWarnings("unchecked")
			L state = (L) params[0];

			Set<SymbolicExpression> toProcess = new HashSet<>();
			for (SymbolicExpression loc : arg) {
				loc = loc.removeTypingExpressions();
				if (loc instanceof Identifier)
					toProcess.addAll(resolveIdentifier(state, (Identifier) loc));
				else
					toProcess.add(loc);
			}

			for (SymbolicExpression loc : toProcess)
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
			@SuppressWarnings("unchecked")
			L state = (L) params[0];

			Set<SymbolicExpression> toProcess = new HashSet<>();
			for (SymbolicExpression rec : arg) {
				rec = rec.removeTypingExpressions();
				if (rec instanceof Identifier)
					toProcess.addAll(resolveIdentifier(state, (Identifier) rec));
				else
					toProcess.add(rec);
			}

			for (SymbolicExpression ref : toProcess)
				if (ref instanceof MemoryPointer)
					result.add(((MemoryPointer) ref).getReferencedLocation());
				else if (ref instanceof Identifier) {
					// this could be aliasing!
					Identifier id = (Identifier) ref;
					if (state.getKeys().contains(id))
						result.addAll(resolveIdentifier(state, id));
					else if (id instanceof Variable) {
						// this is a variable from the program that we know
						// nothing about
						CodeLocation loc = expression.getCodeLocation();
						AllocationSite site;
						if (id.getStaticType().isPointerType())
							site = new HeapAllocationSite(
									id.getStaticType(),
									"unknown@" + id.getName(),
									true,
									loc);
						else if (id.getStaticType().isInMemoryType() || id.getStaticType().isUntyped())
							site = new StackAllocationSite(
									id.getStaticType(),
									"unknown@" + id.getName(),
									true,
									loc);
						else
							throw new SemanticException(
									"The type " + id.getStaticType()
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
			return new ExpressionSet(expression);
		}

		@Override
		public ExpressionSet visit(
				PushAny expression,
				Object... params)
				throws SemanticException {
			if (expression.getStaticType().isPointerType()) {
				Type inner = expression.getStaticType().asPointerType().getInnerType();
				CodeLocation loc = expression.getCodeLocation();
				HeapAllocationSite site = new HeapAllocationSite(
						inner,
						"unknown@" + loc.getCodeLocation(),
						false,
						loc);
				return new ExpressionSet(new MemoryPointer(expression.getStaticType(), site, loc));
			} else if (expression.getStaticType().isInMemoryType()) {
				Type type = expression.getStaticType();
				CodeLocation loc = expression.getCodeLocation();
				StackAllocationSite site = new StackAllocationSite(
						type,
						"unknown@" + loc.getCodeLocation(),
						false,
						loc);
				return new ExpressionSet(new MemoryPointer(expression.getStaticType(), site, loc));
			}
			return new ExpressionSet(expression);
		}

	}

	@Override
	public Satisfiability alias(
			L state,
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isTop())
			return Satisfiability.UNKNOWN;
		if (state.isBottom())
			return Satisfiability.BOTTOM;

		boolean atLeastOne = false;
		boolean all = true;

		ExpressionSet xrs = rewrite(state, x, pp, oracle);
		ExpressionSet yrs = rewrite(state, y, pp, oracle);

		for (SymbolicExpression xr : xrs)
			for (SymbolicExpression yr : yrs)
				if (xr instanceof MemoryPointer && yr instanceof MemoryPointer) {
					HeapLocation xloc = ((MemoryPointer) xr).getReferencedLocation();
					HeapLocation yloc = ((MemoryPointer) yr).getReferencedLocation();
					if (xloc.equals(yloc)) {
						atLeastOne = true;
						all &= true;
					} else
						all = false;
				} else
					// they cannot be alias
					all = false;

		if (all && atLeastOne)
			return Satisfiability.SATISFIED;
		else if (atLeastOne)
			return Satisfiability.UNKNOWN;
		else
			return Satisfiability.NOT_SATISFIED;
	}

	@Override
	public Satisfiability isReachableFrom(
			L state,
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isTop())
			return Satisfiability.UNKNOWN;
		if (state.isBottom())
			return Satisfiability.BOTTOM;

		WorkingSet<SymbolicExpression> ws = VisitOnceFIFOWorkingSet.mk();
		rewrite(state, x, pp, oracle).elements().forEach(ws::push);
		ExpressionSet targets = rewrite(state, y, pp, oracle);

		while (!ws.isEmpty()) {
			SymbolicExpression current = ws.peek();
			if (targets.elements().contains(current))
				return Satisfiability.SATISFIED;

			if (current instanceof Identifier && state.knowsIdentifier((Identifier) current))
				state.getState((Identifier) current).elements().forEach(ws::push);
			else
				rewrite(state, current, pp, oracle).elements().forEach(ws::push);
		}

		return Satisfiability.NOT_SATISFIED;
	}

}
