package it.unive.lisa.analysis.impl.heap.pointbased;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.PointerIdentifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

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

	private HeapReplacement replaceStrong(AllocationSite site, ExternalSet<Type> types, SymbolicExpression extra) {
		AllocationSite weak = new AllocationSite(types, site.getId(), extra, true);
		AllocationSite strong = new AllocationSite(types, site.getId(), extra);
		HeapReplacement replacement = new HeapReplacement();
		replacement.addSource(strong);
		replacement.addTarget(weak);
		return replacement;
	}

	@Override
	protected PointBasedHeap semanticsOf(HeapExpression expression, ProgramPoint pp) throws SemanticException {
		if (expression instanceof AccessChild) {
			AccessChild access = (AccessChild) expression;
			FieldSensitivePointBasedHeap childState = (FieldSensitivePointBasedHeap) smallStepSemantics(
					access.getChild(), pp);

			List<HeapReplacement> substitution = new ArrayList<>(childState.getSubstitution());

			AllocationSite site = (AllocationSite) access.getContainer().getLocation();
			if (site.isWeak())
				for (SymbolicExpression childRewritten : childState.rewrite(access.getChild(), pp))
					substitution.add(replaceStrong(site, access.getTypes(), childRewritten));

			return new FieldSensitivePointBasedHeap(applySubstitutions(childState.heapEnv, substitution), substitution);
		}

		return super.semanticsOf(expression, pp);
	}

	@Override
	public ExpressionSet<ValueExpression> rewrite(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return expression.accept(new Rewriter(), pp);
	}

	private class Rewriter extends PointBasedHeap.Rewriter {
		@Override
		public ExpressionSet<ValueExpression> visit(AccessChild expression, PointerIdentifier receiver,
				ExpressionSet<ValueExpression> child, Object... params) throws SemanticException {
			AccessChild access = (AccessChild) expression;
			Set<ValueExpression> result = new HashSet<>();
			AllocationSite site = (AllocationSite) receiver.getLocation();

			for (SymbolicExpression childRewritten : child)
				result.add(new AllocationSite(access.getTypes(), site.getId(), childRewritten, site.isWeak()));

			return new ExpressionSet<>(result);
		}
	}
}