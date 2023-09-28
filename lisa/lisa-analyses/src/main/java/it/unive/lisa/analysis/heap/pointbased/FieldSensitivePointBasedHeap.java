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
import java.util.function.Predicate;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
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
 * A field-insensitive program point-based {@link AllocationSiteBasedAnalysis}.
 * The implementation follows X. Rival and K. Yi, "Introduction to Static
 * Analysis An Abstract Interpretation Perspective", Section 8.3.4
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 * 
 * @see <a href=
 *          "https://mitpress.mit.edu/books/introduction-static-analysis">https://mitpress.mit.edu/books/introduction-static-analysis</a>
 */
public class FieldSensitivePointBasedHeap extends AllocationSiteBasedAnalysis<FieldSensitivePointBasedHeap> {

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
	public FieldSensitivePointBasedHeap(
			HeapEnvironment<AllocationSites> heapEnv) {
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
	public FieldSensitivePointBasedHeap(
			HeapEnvironment<AllocationSites> heapEnv,
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
	public FieldSensitivePointBasedHeap(
			HeapEnvironment<AllocationSites> heapEnv,
			List<HeapReplacement> replacements,
			GenericMapLattice<AllocationSite, ExpressionSet> fields) {
		super(heapEnv, replacements);
		this.fields = fields;
	}

	@Override
	protected FieldSensitivePointBasedHeap mk(
			HeapEnvironment<AllocationSites> heapEnv,
			List<HeapReplacement> replacements) {
		return new FieldSensitivePointBasedHeap(heapEnv, replacements, fields);
	}

	@Override
	public FieldSensitivePointBasedHeap shallowCopy(
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
		HeapEnvironment<AllocationSites> heap = heapEnv.assign(id, clone, pp, oracle);

		Map<AllocationSite, ExpressionSet> newFields = new HashMap<>(fields.getMap());

		// all the allocation sites fields of star_y
		if (fields.getKeys().contains(site)) {
			for (SymbolicExpression field : fields.getState(site)) {
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
	public FieldSensitivePointBasedHeap smallStepSemantics(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof AccessChild) {
			FieldSensitivePointBasedHeap sss = (FieldSensitivePointBasedHeap) super.smallStepSemantics(
					expression,
					pp,
					oracle);

			AccessChild accessChild = (AccessChild) expression;
			Map<AllocationSite, ExpressionSet> mapping = new HashMap<>(sss.fields.getMap());

			ExpressionSet exprs;
			if (accessChild.getContainer().mightNeedRewriting())
				exprs = rewrite(accessChild.getContainer(), pp, oracle);
			else
				exprs = new ExpressionSet(accessChild.getContainer());

			for (SymbolicExpression rec : exprs)
				if (rec instanceof MemoryPointer) {
					AllocationSite site = (AllocationSite) ((MemoryPointer) rec).getReferencedLocation();
					ExpressionSet childs = rewrite(accessChild.getChild(), pp, oracle);

					for (SymbolicExpression child : childs)
						addField(site, child, mapping);

				} else if (rec instanceof AllocationSite) {
					AllocationSite site = (AllocationSite) rec;
					ExpressionSet childs = rewrite(accessChild.getChild(), pp, oracle);

					for (SymbolicExpression child : childs)
						addField(site, child, mapping);
				}

			return new FieldSensitivePointBasedHeap(heapEnv, heapEnv.getSubstitution(),
					new GenericMapLattice<>(fields.lattice, mapping));
		} else if (expression instanceof MemoryAllocation) {
			String loc = expression.getCodeLocation().getCodeLocation();
			Set<AllocationSite> alreadyAllocated = getAllocatedAt(loc);
			FieldSensitivePointBasedHeap sss = super.smallStepSemantics(expression, pp, oracle);
			HeapEnvironment<AllocationSites> env = sss.heapEnv;

			if (!alreadyAllocated.isEmpty()) {
				// we must turn all these sites to weak ones, including the one
				// about fields
				List<HeapReplacement> replacements = new LinkedList<>();
				for (AllocationSite site : alreadyAllocated) {
					if (!site.isWeak()) {
						HeapReplacement replacement = new HeapReplacement();
						replacement.addSource(site);
						replacement.addTarget(site.toWeak());
						replacements.add(replacement);
					}
					if (fields.getKeys().contains(site))
						for (SymbolicExpression field : fields.getState(site)) {
							AllocationSite withField = site.withField(field);
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

		FieldSensitivePointBasedHeap sss = super.smallStepSemantics(expression, pp, oracle);
		return new FieldSensitivePointBasedHeap(sss.heapEnv, fields);
	}

	private void addField(
			AllocationSite site,
			SymbolicExpression field,
			Map<AllocationSite, ExpressionSet> mapping) {
		Set<SymbolicExpression> tmp = new HashSet<>(mapping.getOrDefault(site, new ExpressionSet()).elements());
		tmp.add(field);
		mapping.put(site, new ExpressionSet(tmp));
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
	 * {@link FieldSensitivePointBasedHeap} domain.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public class Rewriter extends AllocationSiteBasedAnalysis<FieldSensitivePointBasedHeap>.Rewriter {
		
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
					AllocationSite site = (AllocationSite) ((MemoryPointer) rec).getReferencedLocation();
					populate(expression, child, result, site);
				} else if (rec instanceof AllocationSite) {
					AllocationSite site = (AllocationSite) rec;
					populate(expression, child, result, site);
				}

			return new ExpressionSet(result);
		}

		private void populate(
				AccessChild expression,
				ExpressionSet child,
				Set<SymbolicExpression> result,
				AllocationSite site) {
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
				result.add(e);
			}
		}

		@Override
		public ExpressionSet visit(
				MemoryAllocation expression,
				Object... params)
				throws SemanticException {
			String pp = expression.getCodeLocation().getCodeLocation();

			boolean weak;
			if (!getAllocatedAt(pp).isEmpty())
				weak = true;
			else
				weak = false;

			AllocationSite e;
			if (expression.isStackAllocation())
				e = new StackAllocationSite(expression.getStaticType(), pp, weak, expression.getCodeLocation());
			else
				e = new HeapAllocationSite(expression.getStaticType(), pp, weak, expression.getCodeLocation());
			return new ExpressionSet(e);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(fields);
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FieldSensitivePointBasedHeap other = (FieldSensitivePointBasedHeap) obj;
		return Objects.equals(fields, other.fields);
	}

	@Override
	public FieldSensitivePointBasedHeap popScope(
			ScopeToken scope)
			throws SemanticException {
		return new FieldSensitivePointBasedHeap(heapEnv.popScope(scope), fields);
	}

	@Override
	public FieldSensitivePointBasedHeap pushScope(
			ScopeToken scope)
			throws SemanticException {
		return new FieldSensitivePointBasedHeap(heapEnv.pushScope(scope), fields);
	}

	@Override
	public FieldSensitivePointBasedHeap top() {
		return new FieldSensitivePointBasedHeap(heapEnv.top(), Collections.emptyList(), fields.top());
	}

	@Override
	public boolean isTop() {
		return heapEnv.isTop() && fields.isTop();
	}

	@Override
	public FieldSensitivePointBasedHeap bottom() {
		return new FieldSensitivePointBasedHeap(heapEnv.bottom(), Collections.emptyList(), fields.bottom());
	}

	@Override
	public boolean isBottom() {
		return heapEnv.isBottom() && fields.isBottom();
	}

	@Override
	public FieldSensitivePointBasedHeap lubAux(
			FieldSensitivePointBasedHeap other)
			throws SemanticException {
		return new FieldSensitivePointBasedHeap(heapEnv.lub(other.heapEnv),
				Collections.emptyList(),
				fields.lub(other.fields));
	}

	@Override
	public FieldSensitivePointBasedHeap glbAux(
			FieldSensitivePointBasedHeap other)
			throws SemanticException {
		return new FieldSensitivePointBasedHeap(heapEnv.glb(other.heapEnv),
				Collections.emptyList(),
				fields.glb(other.fields));
	}

	@Override
	public boolean lessOrEqualAux(
			FieldSensitivePointBasedHeap other)
			throws SemanticException {
		return heapEnv.lessOrEqual(other.heapEnv) && fields.lessOrEqual(other.fields);
	}

	@Override
	public FieldSensitivePointBasedHeap forgetIdentifier(
			Identifier id)
			throws SemanticException {
		return new FieldSensitivePointBasedHeap(heapEnv.forgetIdentifier(id), fields);
	}

	@Override
	public FieldSensitivePointBasedHeap forgetIdentifiersIf(
			Predicate<Identifier> test)
			throws SemanticException {
		return new FieldSensitivePointBasedHeap(heapEnv.forgetIdentifiersIf(test), fields);
	}
}
