package it.unive.lisa.analysis.heap.pointbased;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
	private final Map<AllocationSite, Set<SymbolicExpression>> fields;

	/**
	 * Builds a new instance of field-sensitive point-based heap.
	 */
	public FieldSensitivePointBasedHeap() {
		super();
		this.fields = new HashMap<AllocationSite, Set<SymbolicExpression>>();

	}

	/**
	 * Builds a new instance of field-sensitive point-based heap from its heap
	 * environment.
	 * 
	 * @param heapEnv the heap environment that this instance tracks
	 */
	public FieldSensitivePointBasedHeap(HeapEnvironment<AllocationSites> heapEnv) {
		this(heapEnv, new HashMap<>());
	}

	/**
	 * Builds a new instance of field-sensitive point-based heap from its heap
	 * environment.
	 * 
	 * @param heapEnv the heap environment that this instance tracks
	 */
	public FieldSensitivePointBasedHeap(HeapEnvironment<AllocationSites> heapEnv,
			Map<AllocationSite, Set<SymbolicExpression>> fields) {
		super(heapEnv);
		this.fields = fields;
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
					if (star_y instanceof StaticAllocationSite) {
						// no aliasing: star_y must be cloned and the clone must
						// be assigned to id
						StaticAllocationSite clone = new StaticAllocationSite(star_y.getStaticType(),
								id.getCodeLocation().toString(), star_y.isWeak(), id.getCodeLocation());
						HeapEnvironment<AllocationSites> heap = sss.heapEnv.assign(id, clone, pp);
						result = result.lub(from(new PointBasedHeap(heap)));

						if (fields.containsKey(star_y))
							for (SymbolicExpression field : fields.get(star_y)) {
								HeapReplacement replacement = new HeapReplacement();
								StaticAllocationSite cloneWithField = new StaticAllocationSite(field.getStaticType(),
										id.getCodeLocation().toString(), field, star_y.isWeak(), id.getCodeLocation());

								StaticAllocationSite star_yWithField = new StaticAllocationSite(field.getStaticType(),
										star_y.getCodeLocation().toString(), field, star_y.isWeak(),
										star_y.getCodeLocation());
								replacement.addSource(star_yWithField);
								replacement.addTarget(cloneWithField);
								replacement.addTarget(star_yWithField);

								result.getSubstitution().add(replacement);
							}
					} else {
						// aliasing: id and star_y points to the same object
						HeapEnvironment<AllocationSites> heap = sss.heapEnv.assign(id, star_y, pp);
						result = result.lub(from(new PointBasedHeap(heap)));
					}
				}
			} else
				result = result.lub(sss);

		return result;
	}

	@Override
	public FieldSensitivePointBasedHeap from(PointBasedHeap original) {
		return new FieldSensitivePointBasedHeap(original.heapEnv);
	}

	@Override
	public ExpressionSet<ValueExpression> rewrite(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return expression.accept(new Rewriter());
	}

	/**
	 * A {@link it.unive.lisa.analysis.heap.BaseHeapDomain.Rewriter} for the
	 * {@link FieldSensitivePointBasedHeap} domain.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public class Rewriter extends PointBasedHeap.Rewriter {

		@Override
		public ExpressionSet<ValueExpression> visit(AccessChild expression, ExpressionSet<ValueExpression> receiver,
				ExpressionSet<ValueExpression> child, Object... params) throws SemanticException {
			Set<ValueExpression> result = new HashSet<>();

			for (ValueExpression rec : receiver)
				if (rec instanceof MemoryPointer) {
					AllocationSite site = (AllocationSite) ((MemoryPointer) rec).getReferencedLocation();
					populate(expression, child, result, site);
				} else if (rec instanceof AllocationSite) {
					AllocationSite site = (AllocationSite) rec;
					populate(expression, child, result, site);
				}

			return new ExpressionSet<>(result);
		}

		private void populate(AccessChild expression, ExpressionSet<ValueExpression> child,
				Set<ValueExpression> result, AllocationSite site) {
			for (SymbolicExpression target : child) {
				AllocationSite e;

				if (site instanceof StaticAllocationSite)
					e = new StaticAllocationSite(
							expression.getStaticType(),
							site.getLocationName(),
							target,
							site.isWeak(),
							site.getCodeLocation());
				else
					e = new DynamicAllocationSite(
							expression.getStaticType(),
							site.getLocationName(),
							target,
							site.isWeak(),
							site.getCodeLocation());

				addField(site, target);
				if (expression.hasRuntimeTypes())
					e.setRuntimeTypes(expression.getRuntimeTypes(null));
				result.add(e);
			}
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapAllocation expression, Object... params)
				throws SemanticException {
			String pp = expression.getCodeLocation().getCodeLocation();

			boolean weak;
			if (alreadyAllocated(pp) != null)
				weak = true;
			else
				weak = false;

			AllocationSite e;
			if (expression.isStaticallyAllocated())
				e = new StaticAllocationSite(expression.getStaticType(), pp, weak, expression.getCodeLocation());
			else
				e = new DynamicAllocationSite(expression.getStaticType(), pp, weak, expression.getCodeLocation());

			if (expression.hasRuntimeTypes())
				e.setRuntimeTypes(expression.getRuntimeTypes(null));
			return new ExpressionSet<>(e);
		}

		private AllocationSite alreadyAllocated(String id) {
			for (AllocationSites set : heapEnv.getValues())
				for (AllocationSite site : set)
					if (site.getLocationName().equals(id))
						return site;

			return null;
		}
	}

	@Override
	public FieldSensitivePointBasedHeap mk(PointBasedHeap reference) {
		return new FieldSensitivePointBasedHeap(reference.heapEnv, ((FieldSensitivePointBasedHeap) reference).fields);

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

	private void addField(AllocationSite site, SymbolicExpression field) {
		if (!fields.containsKey(site))
			fields.put(site, new HashSet<>());
		fields.get(site).add(field);
	}
}
