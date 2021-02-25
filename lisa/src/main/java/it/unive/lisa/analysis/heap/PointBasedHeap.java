package it.unive.lisa.analysis.heap;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.HeapIdentifier;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;

public class PointBasedHeap extends BaseLattice<PointBasedHeap> implements HeapDomain<PointBasedHeap> {

	private static final PointBasedHeap TOP = new PointBasedHeap();

	private static final PointBasedHeap BOTTOM = new PointBasedHeap();

	private final Collection<ValueExpression> rewritten;

	private final HashMap<Identifier, HashSet<ValueExpression>> allocationSites;

	/**
	 * Builds a new instance of PointBasedHeap, with an unique rewritten
	 * expression {@link Skip}.
	 */
	public PointBasedHeap() {
		this(new Skip());
	}

	private PointBasedHeap(ValueExpression rewritten) {
		this(Collections.singleton(rewritten));
	}

	private PointBasedHeap(Collection<ValueExpression> rewritten) {
		this.rewritten = rewritten;
		this.allocationSites = new HashMap<>();
	}

	private PointBasedHeap(Collection<ValueExpression> rewritten, HashMap<Identifier, HashSet<ValueExpression>> allocationSites) {
		this.rewritten = rewritten;
		this.allocationSites = new HashMap<>(allocationSites);
	}

	@Override
	public PointBasedHeap smallStepSemantics(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		if (expression instanceof HeapExpression) {

			if (expression instanceof AccessChild) 
				return new PointBasedHeap(allocationSites.get(((AccessChild) expression).getContainer()), allocationSites);

			if (expression instanceof HeapAllocation || expression instanceof HeapReference) 
				return new PointBasedHeap(Collections.singleton(new HeapIdentifier(expression.getTypes(), pp.toString(), true)), allocationSites);

			return bottom();
		}

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;
			PointBasedHeap sem = smallStepSemantics(unary.getExpression(), pp);
			PointBasedHeap result = bottom();
			for (ValueExpression expr : sem.getRewrittenExpressions())
				result = result.lub(mk(sem, new UnaryExpression(expression.getTypes(), expr, unary.getOperator())));
			return result;
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;
			PointBasedHeap sem1 = smallStepSemantics(binary.getLeft(), pp);
			PointBasedHeap sem2 = sem1.smallStepSemantics(binary.getRight(), pp);
			PointBasedHeap result = bottom();
			for (ValueExpression expr1 : sem1.getRewrittenExpressions())
				for (ValueExpression expr2 : sem2.getRewrittenExpressions())
					result = result.lub(
							mk(sem2, new BinaryExpression(expression.getTypes(), expr1, expr2, binary.getOperator())));
			return result;
		}

		if (expression instanceof ValueExpression)
			return mk(this, (ValueExpression) expression);

		return top();
	}

	@Override
	public PointBasedHeap assign(Identifier id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {

		if (expression instanceof HeapIdentifier) {
			HashMap<Identifier, HashSet<ValueExpression>> sites = new HashMap<>(allocationSites);
			HashSet<ValueExpression> v = new HashSet<>();
			v.add((ValueExpression) expression);
			sites.put(id, v);
			PointBasedHeap res = new PointBasedHeap(rewritten, sites);
			return res;
		}

		return smallStepSemantics(expression, pp);
	}

	@Override
	public PointBasedHeap assume(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		// we just rewrite the expression if needed
		return smallStepSemantics(expression, pp);
	}

	@Override
	public PointBasedHeap forgetIdentifier(Identifier id) throws SemanticException {
		return new PointBasedHeap(rewritten, allocationSites);
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		// we leave the decision to the value domain
		return Satisfiability.UNKNOWN;
	}

	@Override
	public String representation() {
		HashSet<ValueExpression> res = new HashSet<ValueExpression>();
		for (HashSet<ValueExpression> s : allocationSites.values())
			res.addAll(s);
		return res.toString();
	}

	@Override
	public PointBasedHeap top() {
		return TOP;
	}

	@Override
	public PointBasedHeap bottom() {
		return BOTTOM;
	}

	@Override
	public Collection<ValueExpression> getRewrittenExpressions() {
		return rewritten;
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return Collections.emptyList();
	}

	protected PointBasedHeap mk(PointBasedHeap reference, ValueExpression expression) {
		return new PointBasedHeap(Collections.singleton(expression), reference.allocationSites);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected PointBasedHeap lubAux(PointBasedHeap other) throws SemanticException {
		Collection<ValueExpression> rewritten = (CollectionUtils.union(this.rewritten, other.rewritten));
		HashMap<Identifier, HashSet<ValueExpression>> sites = new HashMap<Identifier, HashSet<ValueExpression>>(allocationSites);

		for (Map.Entry<Identifier, HashSet<ValueExpression>> e : other.allocationSites.entrySet())
			if (sites.containsKey(e.getKey())) 
				sites.get(e.getKey()).addAll(e.getValue());
			else
				sites.put(e.getKey(), new HashSet<>(e.getValue()));

		PointBasedHeap res = new PointBasedHeap(rewritten, sites);
		return res;
	}

	@Override
	protected PointBasedHeap wideningAux(PointBasedHeap other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected boolean lessOrEqualAux(PointBasedHeap other) throws SemanticException {
		if (other.allocationSites.keySet().containsAll(allocationSites.keySet())) {
			for (Map.Entry<Identifier, HashSet<ValueExpression>> e : other.allocationSites.entrySet())
				if (!e.getValue().containsAll(allocationSites.get(e.getKey())))
					return false;
			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((allocationSites == null) ? 0 : allocationSites.hashCode());
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
		if (allocationSites == null) {
			if (other.allocationSites != null)
				return false;
		} else if (!allocationSites.equals(other.allocationSites))
			return false;
		if (rewritten == null) {
			if (other.rewritten != null)
				return false;
		} else if (!rewritten.equals(other.rewritten))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return representation();
	}
}
