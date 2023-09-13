package it.unive.lisa.analysis.heap.pointbased;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.GenericMapLattice;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;

/**
 * A field-sensitive point-based heap implementation that abstracts heap
 * locations depending on their allocation sites, namely the position of the
 * code where heap locations are generated. All heap locations that are
 * generated at the same allocation sites are abstracted into a single unique
 * heap identifier. The analysis is field-sensitive in the sense that all the
 * field accesses, with the same field, to a specific allocation site are
 * abstracted into a single heap identifier. The implementation follows X. Rival
 * and K. Yi, "Introduction to Static Analysis An Abstract Interpretation
 * Perspective", Section 8.3.4
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 * 
 * @see <a href=
 *          "https://mitpress.mit.edu/books/introduction-static-analysis">https://mitpress.mit.edu/books/introduction-static-analysis</a>
 */
public class FieldSensitivePointBasedHeap extends PointBasedHeap {

	/**
	 * Tracks the fields of each allocation site.
	 */
	private final GenericMapLattice<AllocationSite, ExpressionSet> fields;

	/**
	 * Builds a new instance of field-sensitive point-based heap.
	 */
	public FieldSensitivePointBasedHeap() {
		super();
		this.fields = new GenericMapLattice<AllocationSite, ExpressionSet>(new ExpressionSet()).top();
	}

	/**
	 * Builds a new instance of field-sensitive point-based heap from its heap
	 * environment.
	 * 
	 * @param heapEnv the heap environment that this instance tracks
	 */
	public FieldSensitivePointBasedHeap(HeapEnvironment<AllocationSites> heapEnv) {
		this(heapEnv, new GenericMapLattice<AllocationSite, ExpressionSet>(new ExpressionSet()).top());
	}

	/**
	 * Builds a new instance of field-sensitive point-based heap from its heap
	 * environment.
	 * 
	 * @param heapEnv the heap environment that this instance tracks
	 * @param fields  the mapping between allocation sites and their fields that
	 *                    this instance tracks
	 */
	public FieldSensitivePointBasedHeap(HeapEnvironment<AllocationSites> heapEnv,
			GenericMapLattice<AllocationSite, ExpressionSet> fields) {
		super(heapEnv);
		this.fields = fields;
	}

	/**
	 * Builds a new instance of field-sensitive point-based heap from its heap
	 * environment, its replacements, and its field mapping.
	 * 
	 * @param heapEnv      the heap environment that this instance tracks
	 * @param replacements the heap replacements
	 * @param fields       the mapping between allocation sites and their fields
	 *                         that this instance tracks
	 */
	public FieldSensitivePointBasedHeap(HeapEnvironment<AllocationSites> heapEnv, List<HeapReplacement> replacements,
			GenericMapLattice<AllocationSite, ExpressionSet> fields) {
		super(heapEnv, replacements);
		this.fields = fields;
	}

	@Override
	protected FieldSensitivePointBasedHeap buildHeapAfterAssignment(PointBasedHeap sss,
			List<HeapReplacement> replacements) {
		return new FieldSensitivePointBasedHeap(sss.heapEnv, replacements,
				((FieldSensitivePointBasedHeap) sss).fields);
	}

	@Override
	public FieldSensitivePointBasedHeap nonAliasedAssignment(Identifier id, StackAllocationSite site,
			PointBasedHeap pb,
			ProgramPoint pp, List<HeapReplacement> replacements)
			throws SemanticException {
		// no aliasing: star_y must be cloned and the clone must
		// be assigned to id
		StackAllocationSite clone = new StackAllocationSite(site.getStaticType(),
				id.getCodeLocation().toString(), site.isWeak(), id.getCodeLocation());
		HeapEnvironment<AllocationSites> heap = pb.heapEnv.assign(id, clone, pp);

		FieldSensitivePointBasedHeap fpb = (FieldSensitivePointBasedHeap) pb;
		Map<AllocationSite, ExpressionSet> newFields = new HashMap<>(fpb.fields.getMap());

		// all the allocation sites fields of star_y
		if (fpb.fields.getKeys().contains(site)) {
			for (SymbolicExpression field : fpb.fields.getState(site)) {
				StackAllocationSite cloneWithField = new StackAllocationSite(field.getStaticType(),
						id.getCodeLocation().toString(), field, site.isWeak(), id.getCodeLocation());

				StackAllocationSite star_yWithField = new StackAllocationSite(field.getStaticType(),
						site.getCodeLocation().toString(), field, site.isWeak(),
						site.getCodeLocation());
				HeapReplacement replacement = new HeapReplacement();
				replacement.addSource(star_yWithField);
				replacement.addTarget(cloneWithField);
				replacement.addTarget(star_yWithField);

				// need to update also the fields of the clone
				addField(clone, field, newFields);

				replacements.add(replacement);
			}
		}

		// need to be replaced also the allocation site (needed for type
		// analysis)
		HeapReplacement replacement = new HeapReplacement();
		replacement.addSource(site);
		replacement.addTarget(clone);
		replacement.addTarget(site);
		replacements.add(replacement);

		return new FieldSensitivePointBasedHeap(heap, new GenericMapLattice<>(fields.lattice, newFields));
	}

	@Override
	public FieldSensitivePointBasedHeap from(PointBasedHeap original) {
		return new FieldSensitivePointBasedHeap(original.heapEnv, fields);
	}

	@Override
	public ExpressionSet rewrite(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return expression.accept(new Rewriter());
	}

	@Override
	public FieldSensitivePointBasedHeap smallStepSemantics(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		if (expression instanceof AccessChild) {
			FieldSensitivePointBasedHeap sss = (FieldSensitivePointBasedHeap) super.smallStepSemantics(expression, pp);

			AccessChild accessChild = (AccessChild) expression;
			Map<AllocationSite, ExpressionSet> mapping = new HashMap<>(sss.fields.getMap());

			ExpressionSet exprs = rewrite(accessChild.getContainer(), pp);
			for (SymbolicExpression rec : exprs)
				if (rec instanceof MemoryPointer) {
					AllocationSite site = (AllocationSite) ((MemoryPointer) rec).getReferencedLocation();
					ExpressionSet childs = rewrite(accessChild.getChild(), pp);

					for (SymbolicExpression child : childs)
						addField(site, child, mapping);

				} else if (rec instanceof AllocationSite) {
					AllocationSite site = (AllocationSite) rec;
					ExpressionSet childs = rewrite(accessChild.getChild(), pp);

					for (SymbolicExpression child : childs)
						addField(site, child, mapping);
				}

			return new FieldSensitivePointBasedHeap(heapEnv, heapEnv.getSubstitution(),
					new GenericMapLattice<>(fields.lattice, mapping));
		} else if (expression instanceof MemoryAllocation) {
			String loc = expression.getCodeLocation().getCodeLocation();
			Set<Pair<Identifier, AllocationSite>> alreadyAllocated = getAlreadyAllocated(loc);
			PointBasedHeap sss = super.smallStepSemantics(expression, pp);
			HeapEnvironment<AllocationSites> env = sss.heapEnv;

			if (!alreadyAllocated.isEmpty()) {
				// we must turn all these sites to weak ones, including the one
				// about fields
				List<HeapReplacement> replacements = new LinkedList<>();
				for (Pair<Identifier, AllocationSite> site : alreadyAllocated) {
					if (!site.getValue().isWeak()) {
						HeapReplacement replacement = new HeapReplacement();
						replacement.addSource(site.getValue());
						replacement.addTarget(site.getValue().toWeak());
						replacements.add(replacement);
					}
					if (fields.getKeys().contains(site.getValue()))
						for (SymbolicExpression field : fields.getState(site.getValue())) {
							AllocationSite withField = site.getValue().withField(field);
							if (!withField.isWeak()) {
								HeapReplacement replacement = new HeapReplacement();
								replacement.addSource(withField);
								replacement.addTarget(withField.toWeak());
								replacements.add(replacement);
							}
						}
				}

				if (!replacements.isEmpty()) {
					// we must apply the replacements to our mapping as well
					Map<Identifier, AllocationSites> map = new HashMap<>(env.getMap());
					for (Entry<Identifier, AllocationSites> entry : env) {
						Identifier id = entry.getKey();
						AllocationSites sites = entry.getValue();
						for (HeapReplacement repl : replacements) {
							if (repl.getSources().contains(id))
								// these are all one-to-one replacements
								id = repl.getTargets().iterator().next();
							sites = sites.applyReplacement(repl, pp);
						}
						map.put(id, sites);
					}
					env = new HeapEnvironment<>(env.lattice, map);
				}

				return new FieldSensitivePointBasedHeap(env, replacements, fields);
			}
		}

		PointBasedHeap sss = super.smallStepSemantics(expression, pp);
		return new FieldSensitivePointBasedHeap(sss.heapEnv, fields);
	}

	/**
	 * A {@link it.unive.lisa.analysis.heap.BaseHeapDomain.Rewriter} for the
	 * {@link FieldSensitivePointBasedHeap} domain.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public class Rewriter extends PointBasedHeap.Rewriter {

		@Override
		public ExpressionSet visit(AccessChild expression, ExpressionSet receiver,
				ExpressionSet child, Object... params) throws SemanticException {
			Set<SymbolicExpression> result = new HashSet<>();

			for (SymbolicExpression rec : receiver)
				if (rec instanceof MemoryPointer) {
					AllocationSite site = (AllocationSite) ((MemoryPointer) rec).getReferencedLocation();
					populate(expression, child, result, site);
				} else if (rec instanceof AllocationSite) {
					AllocationSite site = (AllocationSite) rec;
					populate(expression, child, result, site);
				}

			return new ExpressionSet(result);
		}

		private void populate(AccessChild expression, ExpressionSet child,
				Set<SymbolicExpression> result, AllocationSite site) {
			for (SymbolicExpression target : child) {
				AllocationSite e;

				if (site instanceof StackAllocationSite)
					e = new StackAllocationSite(
							expression.getStaticType(),
							site.getLocationName(),
							target,
							site.isWeak(),
							site.getCodeLocation());
				else
					e = new HeapAllocationSite(
							expression.getStaticType(),
							site.getLocationName(),
							target,
							site.isWeak(),
							site.getCodeLocation());

				if (expression.hasRuntimeTypes())
					e.setRuntimeTypes(expression.getRuntimeTypes(null));
				result.add(e);
			}
		}

		@Override
		public ExpressionSet visit(MemoryAllocation expression, Object... params)
				throws SemanticException {
			String pp = expression.getCodeLocation().getCodeLocation();

			boolean weak;
			if (!getAlreadyAllocated(pp).isEmpty())
				weak = true;
			else
				weak = false;

			AllocationSite e;
			if (expression.isStackAllocation())
				e = new StackAllocationSite(expression.getStaticType(), pp, weak, expression.getCodeLocation());
			else
				e = new HeapAllocationSite(expression.getStaticType(), pp, weak, expression.getCodeLocation());

			if (expression.hasRuntimeTypes())
				e.setRuntimeTypes(expression.getRuntimeTypes(null));
			return new ExpressionSet(e);
		}
	}

	@Override
	public FieldSensitivePointBasedHeap mk(PointBasedHeap reference) {
		if (reference instanceof FieldSensitivePointBasedHeap)
			return new FieldSensitivePointBasedHeap(reference.heapEnv,
					((FieldSensitivePointBasedHeap) reference).fields);
		else
			return new FieldSensitivePointBasedHeap(reference.heapEnv);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(fields);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FieldSensitivePointBasedHeap other = (FieldSensitivePointBasedHeap) obj;
		return Objects.equals(fields, other.fields);
	}

	private void addField(AllocationSite site, SymbolicExpression field,
			Map<AllocationSite, ExpressionSet> mapping) {
		Set<SymbolicExpression> tmp = new HashSet<>(mapping.getOrDefault(site, new ExpressionSet()).elements());
		tmp.add(field);
		mapping.put(site, new ExpressionSet(tmp));
	}

	@Override
	public FieldSensitivePointBasedHeap popScope(ScopeToken scope) throws SemanticException {
		return new FieldSensitivePointBasedHeap(heapEnv.popScope(scope), fields);
	}

	@Override
	public FieldSensitivePointBasedHeap pushScope(ScopeToken scope) throws SemanticException {
		return new FieldSensitivePointBasedHeap(heapEnv.pushScope(scope), fields);
	}

	@Override
	public PointBasedHeap top() {
		return new FieldSensitivePointBasedHeap(heapEnv.top(), Collections.emptyList(), fields.top());
	}

	@Override
	public boolean isTop() {
		return heapEnv.isTop() && fields.isTop();
	}

	@Override
	public PointBasedHeap bottom() {
		return new FieldSensitivePointBasedHeap(heapEnv.bottom(), Collections.emptyList(), fields.bottom());
	}

	@Override
	public boolean isBottom() {
		return heapEnv.isBottom() && fields.isBottom();
	}

	@Override
	public PointBasedHeap lubAux(PointBasedHeap other) throws SemanticException {
		if (other instanceof FieldSensitivePointBasedHeap)
			return new FieldSensitivePointBasedHeap(heapEnv.lub(other.heapEnv),
					Collections.emptyList(),
					fields.lub(((FieldSensitivePointBasedHeap) other).fields));
		else
			return new FieldSensitivePointBasedHeap(heapEnv.lub(other.heapEnv),
					Collections.emptyList(),
					fields);
	}

	@Override
	public PointBasedHeap glbAux(PointBasedHeap other) throws SemanticException {
		if (other instanceof FieldSensitivePointBasedHeap)
			return new FieldSensitivePointBasedHeap(heapEnv.glb(other.heapEnv),
					Collections.emptyList(),
					fields.glb(((FieldSensitivePointBasedHeap) other).fields));
		else
			return new FieldSensitivePointBasedHeap(heapEnv.glb(other.heapEnv),
					Collections.emptyList(),
					fields.bottom());
	}

	@Override
	public boolean lessOrEqualAux(PointBasedHeap other) throws SemanticException {
		if (other instanceof FieldSensitivePointBasedHeap)
			return heapEnv.lessOrEqual(other.heapEnv)
					&& fields.lessOrEqual(((FieldSensitivePointBasedHeap) other).fields);
		else
			return false;
	}

}
