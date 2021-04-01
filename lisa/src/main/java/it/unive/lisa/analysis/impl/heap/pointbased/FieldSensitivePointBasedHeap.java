package it.unive.lisa.analysis.impl.heap.pointbased;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;

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

	private FieldSensitivePointBasedHeap(Collection<ValueExpression> rewritten,
			HeapEnvironment<AllocationSites> allocationSites) {
		super(rewritten, allocationSites, Collections.emptyList());
	}

	@Override
	protected FieldSensitivePointBasedHeap from(PointBasedHeap original) {
		return new FieldSensitivePointBasedHeap(original.getRewrittenExpressions(), original.heapEnv);
	}

	@Override
	protected PointBasedHeap semanticsOf(HeapExpression expression, ProgramPoint pp) throws SemanticException {
		if (expression instanceof AccessChild) {
			AccessChild access = (AccessChild) expression;
			FieldSensitivePointBasedHeap containerState = (FieldSensitivePointBasedHeap) smallStepSemantics(
					access.getContainer(), pp);
			FieldSensitivePointBasedHeap childState = (FieldSensitivePointBasedHeap) containerState.smallStepSemantics(
					access.getChild(),	pp);

			Set<ValueExpression> result = new HashSet<>();
			for (SymbolicExpression exp : containerState.getRewrittenExpressions()) {
				if (exp instanceof Variable) {
					AllocationSites expHids = childState.heapEnv.getState((Identifier) exp);
					if (!(expHids.isBottom()))
						for (AllocationSite hid : expHids)
							for (SymbolicExpression childRewritten : childState.getRewrittenExpressions())
								result.add(new AllocationSite(access.getTypes(), hid.getId(), childRewritten));
				} else if (exp instanceof AllocationSite) {
					for (SymbolicExpression childRewritten : childState.getRewrittenExpressions())
						result.add(new AllocationSite(access.getTypes(), ((AllocationSite) exp).getId(),
								childRewritten));
				} else if (exp instanceof HeapLocation) {
					result.add((ValueExpression) exp);
				}
			}

			return new FieldSensitivePointBasedHeap(result, childState.heapEnv);
		}

		return super.semanticsOf(expression, pp);
	}
}