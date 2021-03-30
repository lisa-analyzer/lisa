package it.unive.lisa.analysis.heap.pointbased;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections.CollectionUtils;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.BaseHeapDomain;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Call;
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
import it.unive.lisa.util.collections.Utils;

/**
 * A field-insensitive point-based heap implementation that abstracts heap
 * locations depending on their allocation sites, namely the position of the
 * code where heap locations are generated. All heap locations that are
 * generated at the same allocation sites are abstracted into a single unique
 * heap identifier. The implementation follows X. Rival and K. Yi, "Introduction
 * to Static Analysis An Abstract Interpretation Perspective", Section 8.3.4
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 * 
 * @see <a href=
 *          "https://mitpress.mit.edu/books/introduction-static-analysis">https://mitpress.mit.edu/books/introduction-static-analysis</a>
 */
public class PointBasedHeap extends BaseHeapDomain<PointBasedHeap> {

	private final Collection<ValueExpression> rewritten;

	/**
	 * An heap environment tracking which allocation sites are associated to
	 * each identifier.
	 */
	protected final HeapEnvironment<AllocationSites> heapEnv;

	/**
	 * Builds a new instance of field-insensitive point-based heap, with an
	 * unique rewritten expression {@link Skip}.
	 */
	public PointBasedHeap() {
		this(Collections.singleton(new Skip()), new HeapEnvironment<AllocationSites>(new AllocationSites()));
	}

	/**
	 * Builds a new instance of field-insensitive point-based heap from its
	 * rewritten expressions and heap environment.
	 * 
	 * @param rewritten the collection of rewritten expressions
	 * @param heapEnv   the heap environment that this instance tracks
	 */
	protected PointBasedHeap(Collection<ValueExpression> rewritten,
			HeapEnvironment<AllocationSites> heapEnv) {
		this.rewritten = rewritten;
		this.heapEnv = heapEnv;
	}

	/**
	 * Builds a point-based heap from a reference one.
	 * 
	 * @param original reference point-based heap
	 * 
	 * @return a point-based heap build from the original one
	 */
	protected PointBasedHeap from(PointBasedHeap original) {
		return original;
	}

	@Override
	public PointBasedHeap assign(Identifier id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {

		if (expression instanceof AllocationSite)
			return from(new PointBasedHeap(Collections.singleton((AllocationSite) expression),
					heapEnv.assign(id, expression, pp)));

		return smallStepSemantics(expression, pp);
	}

	@Override
	public PointBasedHeap assume(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		// we just rewrite the expression if needed
		return smallStepSemantics(expression, pp);
	}

	@Override
	public PointBasedHeap forgetIdentifier(Identifier id) throws SemanticException {
		return from(new PointBasedHeap(rewritten, heapEnv.forgetIdentifier(id)));
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		// we leave the decision to the value domain
		return Satisfiability.UNKNOWN;
	}

	@Override
	public String representation() {
		if (isTop())
			return Lattice.TOP_STRING;

		if (isBottom())
			return Lattice.BOTTOM_STRING;

		Collection<String> res = new TreeSet<String>(
				(l, r) -> Utils.nullSafeCompare(true, l, r, (ll, rr) -> ll.toString().compareTo(rr.toString())));
		for (Identifier id : heapEnv.getKeys())
			for (HeapIdentifier hid : heapEnv.getState(id))
				res.add(hid.toString());

		return res.toString();
	}

	@Override
	public PointBasedHeap top() {
		return from(new PointBasedHeap(Collections.emptySet(), heapEnv.top()));
	}

	@Override
	public boolean isTop() {
		return rewritten.isEmpty() && heapEnv.isTop();
	}

	@Override
	public PointBasedHeap bottom() {
		return from(new PointBasedHeap(Collections.emptySet(), heapEnv.bottom()));
	}

	@Override
	public boolean isBottom() {
		return rewritten.isEmpty() && heapEnv.isBottom();
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
		return from(new PointBasedHeap(Collections.singleton(expression), reference.heapEnv));
	}

	@Override
	@SuppressWarnings("unchecked")
	protected PointBasedHeap lubAux(PointBasedHeap other) throws SemanticException {
		return from(new PointBasedHeap(CollectionUtils.union(this.rewritten, other.rewritten),
				heapEnv.lub(other.heapEnv)));
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
			PointBasedHeap containerState = smallStepSemantics((((AccessChild) expression).getContainer()), pp);
			PointBasedHeap childState = containerState.smallStepSemantics((((AccessChild) expression).getChild()),
					pp);

			Set<ValueExpression> result = new HashSet<>();
			for (SymbolicExpression exp : containerState.getRewrittenExpressions()) {
				AllocationSites expHids = childState.heapEnv.getState((Identifier) exp);
				if (!(expHids.isBottom()))
					for (AllocationSite hid : expHids)
						result.add(new AllocationSite(expression.getTypes(), hid.getId()));
			}

			return from(new PointBasedHeap(result, childState.heapEnv));
		}

		if (expression instanceof HeapAllocation) {
			HeapIdentifier id = new AllocationSite(expression.getTypes(), pp.getLocation().getCodeLocation());
			return from(new PointBasedHeap(Collections.singleton(id), heapEnv));
		}

		if (expression instanceof HeapReference) {
			HeapReference heapRef = (HeapReference) expression;
			for (Identifier id : heapEnv.getKeys())
				if (id instanceof ValueIdentifier && heapRef.getName().equals(id.getName()))
					return from(new PointBasedHeap(Collections.singleton(id), heapEnv));
		}

		return top();
	}

	@Override
	public PointBasedHeap pushScope(Call scope) throws SemanticException {
		Collection<ValueExpression> rew = new HashSet<>();
		for (ValueExpression ve : rewritten)
			rew.add((ValueExpression) ve.pushScope(scope));
		return from(new PointBasedHeap(rew, heapEnv.pushScope(scope)));
	}

	@Override
	public PointBasedHeap popScope(Call scope) throws SemanticException {
		Collection<ValueExpression> rew = new HashSet<>();
		for (ValueExpression ve : rewritten)
			rew.add((ValueExpression) ve.popScope(scope));
		return from(new PointBasedHeap(rew, heapEnv.pushScope(scope)));
	}
}
