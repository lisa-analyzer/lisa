package it.unive.lisa.analysis.heap.pointbased;

import it.unive.lisa.analysis.BaseHeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.HeapIdentifier;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.ValueIdentifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.collections.CollectionUtils;

/**
 * A point-based heap implementation that abstracts heap locations depending on
 * their allocation sites, namely the position of the code where heap locations
 * are generated. All heap locations that are generated at the same allocation
 * sites are abstracted into a single unique heap identifier. The implementation
 * follows X. Rival and K. Yi, "Introduction to Static Analysis An Abstract
 * Interpretation Perspective", Section 8.3.4
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 * 
 * @see <a href=
 *          "https://mitpress.mit.edu/books/introduction-static-analysis">https://mitpress.mit.edu/books/introduction-static-analysis</a>
 */
public class PointBasedHeap extends BaseHeapDomain<PointBasedHeap> {

	private final boolean isFieldSensitive;

	private final Collection<ValueExpression> rewritten;

	private final HeapEnvironment<HeapIdentifierSetLattice> heapEnv;

	/**
	 * Builds a new instance of field-insensitive point-based heap, with an
	 * unique rewritten expression {@link Skip}.
	 */
	public PointBasedHeap() {
		this(new Skip(), false);
	}

	/**
	 * Builds a new instance of point-based heap, with an unique rewritten
	 * expressions {@link Skip}.
	 * 
	 * @param isFieldSensitive specifies is this heap domain is field sensitive.
	 */
	public PointBasedHeap(boolean isFieldSensitive) {
		this(new Skip(), isFieldSensitive);
	}

	private PointBasedHeap(ValueExpression rewritten, boolean isFieldSensitive) {
		this(Collections.singleton(rewritten),
				new HeapEnvironment<HeapIdentifierSetLattice>(new HeapIdentifierSetLattice()), isFieldSensitive);
	}

	private PointBasedHeap(Collection<ValueExpression> rewritten,
			HeapEnvironment<HeapIdentifierSetLattice> allocationSites, boolean isFieldSensitiv) {
		this.rewritten = rewritten;
		this.heapEnv = allocationSites;
		isFieldSensitive = isFieldSensitiv;
	}

	@Override
	public PointBasedHeap assign(Identifier id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {

		if (expression instanceof AllocationSiteHeapIdentifier)
			return new PointBasedHeap(Collections.singleton((AllocationSiteHeapIdentifier) expression),
					heapEnv.assign(id, expression, pp), this.isFieldSensitive);

		return smallStepSemantics(expression, pp);
	}

	@Override
	public PointBasedHeap assume(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		// we just rewrite the expression if needed
		return smallStepSemantics(expression, pp);
	}

	@Override
	public PointBasedHeap forgetIdentifier(Identifier id) throws SemanticException {
		return new PointBasedHeap(rewritten, heapEnv.forgetIdentifier(id), isFieldSensitive);
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		// we leave the decision to the value domain
		return Satisfiability.UNKNOWN;
	}

	@Override
	public String representation() {
		Collection<String> res = new TreeSet<String>();
		for (Identifier id : heapEnv.getKeys())
			for (HeapIdentifier hid : heapEnv.getState(id))
				res.add(hid.toString());
		return res.toString();
	}

	@Override
	public PointBasedHeap top() {
		return new PointBasedHeap(Collections.emptySet(), heapEnv.top(), isFieldSensitive);
	}

	@Override
	public PointBasedHeap bottom() {
		return new PointBasedHeap(Collections.emptySet(), heapEnv.bottom(), isFieldSensitive);
	}

	@Override
	public Collection<ValueExpression> getRewrittenExpressions() {
		return rewritten;
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return Collections.emptyList();
	}

	@Override
	public PointBasedHeap mk(PointBasedHeap reference, ValueExpression expression) {
		return new PointBasedHeap(Collections.singleton(expression), reference.heapEnv, reference.isFieldSensitive);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected PointBasedHeap lubAux(PointBasedHeap other) throws SemanticException {
		return new PointBasedHeap(CollectionUtils.union(this.rewritten, other.rewritten),
				heapEnv.lub(other.heapEnv), isFieldSensitive);
	}

	@Override
	protected PointBasedHeap wideningAux(PointBasedHeap other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected boolean lessOrEqualAux(PointBasedHeap other) throws SemanticException {
		return heapEnv.lessOrEqual(other.heapEnv);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((heapEnv == null) ? 0 : heapEnv.hashCode());
		result = prime * result + ((rewritten == null) ? 0 : rewritten.hashCode());
		return result;
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
		if (heapEnv == null) {
			if (other.heapEnv != null)
				return false;
		} else if (!heapEnv.equals(other.heapEnv))
			return false;
		if (rewritten == null) {
			if (other.rewritten != null)
				return false;
		} else if (!rewritten.equals(other.rewritten))
			return false;
		return true;
	}

	@Override
	protected PointBasedHeap semanticsOf(HeapExpression expression, ProgramPoint pp) throws SemanticException {
		if (expression instanceof AccessChild) {
			PointBasedHeap childState = smallStepSemantics((((AccessChild) expression).getChild()), pp);
			PointBasedHeap containerState = childState.smallStepSemantics((((AccessChild) expression).getContainer()),
					pp);

			Set<ValueExpression> result = new HashSet<>();
			for (SymbolicExpression exp : containerState.getRewrittenExpressions()) {
				HeapIdentifierSetLattice expHids = containerState.heapEnv.getState((Identifier) exp);
				if (!(expHids.isBottom()))
					for (AllocationSiteHeapIdentifier hid : expHids)
						if (isFieldSensitive)
							for (SymbolicExpression childRewritten : childState.getRewrittenExpressions())
								result.add(new AllocationSiteHeapIdentifier(expression.getTypes(),
										hid.getProgramPoint(), childRewritten));
						else
							result.add(new AllocationSiteHeapIdentifier(expression.getTypes(), hid.getProgramPoint()));
			}

			return new PointBasedHeap(result, containerState.heapEnv, containerState.isFieldSensitive);
		}

		if (expression instanceof HeapAllocation) {
			HeapIdentifier id = new AllocationSiteHeapIdentifier(expression.getTypes(), pp);
			return new PointBasedHeap(Collections.singleton(id), heapEnv, isFieldSensitive);
		}

		if (expression instanceof HeapReference) {
			HeapReference heapRef = (HeapReference) expression;
			for (Identifier id : heapEnv.getKeys())
				if (id instanceof ValueIdentifier && heapRef.getName().equals(id.getName()))
					return new PointBasedHeap(Collections.singleton(id), heapEnv, isFieldSensitive);
		}

		return top();
	}
}
