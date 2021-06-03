package it.unive.lisa.analysis.impl.heap.pointbased;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PointerIdentifier;
import it.unive.lisa.symbolic.value.ValueExpression;

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

	private FieldSensitivePointBasedHeap(HeapEnvironment<AllocationSites> allocationSites,
			List<HeapReplacement> substitutions) {
		super(allocationSites, substitutions);
	}

	@Override
	protected FieldSensitivePointBasedHeap from(PointBasedHeap original) {
		return new FieldSensitivePointBasedHeap(original.heapEnv, original.getSubstitution());
	}

	@Override
	public PointBasedHeap assign(Identifier id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {

		FieldSensitivePointBasedHeap sss = (FieldSensitivePointBasedHeap) smallStepSemantics(expression, pp);
		ExpressionSet<ValueExpression> rewrittenExp = sss.rewrite(expression, pp);

		FieldSensitivePointBasedHeap result = (FieldSensitivePointBasedHeap) bottom();
		for (ValueExpression exp : rewrittenExp) {
			if (exp instanceof PointerIdentifier) {
				PointerIdentifier pid = (PointerIdentifier) exp;	
				Identifier v = id instanceof PointerIdentifier ? ((PointerIdentifier) id).getLocation() : id;
				HeapEnvironment<AllocationSites> heap = sss.heapEnv.assign(v, pid.getLocation(), pp);
				result = (FieldSensitivePointBasedHeap) result.lub(from(new FieldSensitivePointBasedHeap(applySubstitutions(heap, sss.getSubstitution()), sss.getSubstitution())));
			} else
				result = (FieldSensitivePointBasedHeap) result.lub(sss);
		}
		
		return result;
	}
	
	@Override
	public ExpressionSet<ValueExpression> rewrite(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return expression.accept(new Rewriter(), pp);
	}

	private class Rewriter extends PointBasedHeap.Rewriter {
		
		@Override
		public ExpressionSet<ValueExpression> visit(AccessChild expression, ExpressionSet<ValueExpression>  receiver,
				ExpressionSet<ValueExpression> child, Object... params) throws SemanticException {
			AccessChild access = (AccessChild) expression;
			Set<ValueExpression> result = new HashSet<>();

			for (SymbolicExpression contRewritten : receiver)
				if (contRewritten instanceof PointerIdentifier) {
					AllocationSite site = (AllocationSite) ((PointerIdentifier) contRewritten).getLocation();
					for (SymbolicExpression childRewritten : child)
						result.add(new AllocationSite(access.getTypes(), site.getId(), childRewritten, site.isWeak()));
				}

			return new ExpressionSet<>(result);
		}
	}
}