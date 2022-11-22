package it.unive.lisa.analysis.heap.pointbased;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.HashSet;
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
	 * Builds a new instance of field-sensitive point-based heap.
	 */
	public FieldSensitivePointBasedHeap() {
		super();
	}

	/**
	 * Builds a new instance of field-sensitive point-based heap from its heap
	 * environment.
	 * 
	 * @param heapEnv the heap environment that this instance tracks
	 */
	public FieldSensitivePointBasedHeap(HeapEnvironment<AllocationSites> heapEnv) {
		super(heapEnv);
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
				AllocationSite e = new AllocationSite(
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
		public ExpressionSet<ValueExpression> visit(HeapAllocation expression, Object... params)
				throws SemanticException {
			String pp = expression.getCodeLocation().getCodeLocation();

			boolean weak;
			if (alreadyAllocated(pp) != null)
				weak = true;
			else
				weak = false;
			AllocationSite e = new AllocationSite(expression.getStaticType(), pp, weak, expression.getCodeLocation());
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
}
