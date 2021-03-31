package it.unive.lisa.analysis.heap.pointbased;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.SetLattice;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.heap.NonRelationalHeapDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;

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
 *      "https://mitpress.mit.edu/books/introduction-static-analysis">https://mitpress.mit.edu/books/introduction-static-analysis</a>
 */
public class PointBasedHeap extends SetLattice<PointBasedHeap, AllocationSite>
		implements NonRelationalHeapDomain<PointBasedHeap> {

	private static final PointBasedHeap TOP = new PointBasedHeap();
	private static final PointBasedHeap BOTTOM = new PointBasedHeap();

	private final Collection<ValueExpression> rewritten;

	/**
	 * Builds a new instance of field-insensitive point-based heap, with an unique
	 * rewritten expression {@link Skip}.
	 */
	public PointBasedHeap() {
		this(Collections.emptySet(), Collections.emptySet());
	}

	/**
	 * Builds a new instance of field-insensitive point-based heap.
	 * 
	 * @param elements  the allocation sites represented by this instance
	 * @param rewritten the collection of rewritten expressions
	 */
	protected PointBasedHeap(Set<AllocationSite> elements, Collection<ValueExpression> rewritten) {
		super(elements);
		this.rewritten = rewritten;
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
	public String representation() {
		return toString();
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

	@Override
	@SuppressWarnings("unchecked")
	protected PointBasedHeap lubAux(PointBasedHeap other) throws SemanticException {
		PointBasedHeap lub = super.lubAux(other);
		return new PointBasedHeap(lub.elements, CollectionUtils.union(this.rewritten, other.rewritten));
	}

	@Override
	protected boolean lessOrEqualAux(PointBasedHeap other) throws SemanticException {
		return super.lessOrEqualAux(other) && other.rewritten.containsAll(rewritten);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((rewritten == null) ? 0 : rewritten.hashCode());
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
		PointBasedHeap other = (PointBasedHeap) obj;
		if (rewritten == null) {
			if (other.rewritten != null)
				return false;
		} else if (!rewritten.equals(other.rewritten))
			return false;
		return true;
	}

	private Collection<ValueExpression> cast(Collection<AllocationSite> sites) {
		return sites.stream().map(ValueExpression.class::cast).collect(Collectors.toList());
	}

	@Override
	public PointBasedHeap eval(SymbolicExpression expression, HeapEnvironment<PointBasedHeap> environment,
			ProgramPoint pp) throws SemanticException {
		if (expression instanceof Variable) {
			PointBasedHeap state = environment.getState((Variable) expression);
			if (state.isTop() || state.isBottom())
				return from(new PointBasedHeap(Collections.emptySet(), Collections.singleton((Variable) expression)));
			return from(new PointBasedHeap(Collections.emptySet(), cast(state.elements)));
		}

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;
			PointBasedHeap sem = eval(unary.getExpression(), environment, pp);
			if (sem.isBottom())
				return sem;
			PointBasedHeap result = bottom();
			for (ValueExpression expr : sem.getRewrittenExpressions())
				result = result.lub(from(new PointBasedHeap(sem.elements,
						Collections.singleton(new UnaryExpression(expression.getTypes(), expr, unary.getOperator())))));
			return result;
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;
			PointBasedHeap sem1 = eval(binary.getLeft(), environment, pp);
			if (sem1.isBottom())
				return sem1;
			PointBasedHeap sem2 = sem1.eval(binary.getRight(), environment, pp);
			if (sem2.isBottom())
				return sem2;
			PointBasedHeap result = bottom();
			for (ValueExpression expr1 : sem1.getRewrittenExpressions())
				for (ValueExpression expr2 : sem2.getRewrittenExpressions())
					result = result.lub(from(new PointBasedHeap(sem2.elements, Collections.singleton(
							new BinaryExpression(expression.getTypes(), expr1, expr2, binary.getOperator())))));
			return result;
		}

		if (expression instanceof TernaryExpression) {
			TernaryExpression ternary = (TernaryExpression) expression;
			PointBasedHeap sem1 = eval(ternary.getLeft(), environment, pp);
			if (sem1.isBottom())
				return sem1;
			PointBasedHeap sem2 = sem1.eval(ternary.getMiddle(), environment, pp);
			if (sem2.isBottom())
				return sem2;
			PointBasedHeap sem3 = sem2.eval(ternary.getRight(), environment, pp);
			if (sem3.isBottom())
				return sem3;
			PointBasedHeap result = bottom();
			for (ValueExpression expr1 : sem1.getRewrittenExpressions())
				for (ValueExpression expr2 : sem2.getRewrittenExpressions())
					for (ValueExpression expr3 : sem3.getRewrittenExpressions())
						result = result.lub(from(new PointBasedHeap(sem3.elements,
								Collections.singleton(new TernaryExpression(expression.getTypes(), expr1, expr2, expr3,
										ternary.getOperator())))));
			return result;
		}

		if (expression instanceof AccessChild) {
			AccessChild access = (AccessChild) expression;
			PointBasedHeap containerState = eval(access.getContainer(), environment, pp);
			PointBasedHeap childState = containerState.eval(access.getChild(), environment, pp);

			Set<ValueExpression> result = new HashSet<>();
			for (SymbolicExpression exp : containerState.getRewrittenExpressions()) {
				if (exp instanceof Variable) {
					PointBasedHeap expHids = environment.getState((Identifier) exp);
					if (!(expHids.isBottom()))
						for (AllocationSite hid : expHids)
							result.add(new AllocationSite(expression.getTypes(), hid.getId()));
				} else if (exp instanceof AllocationSite) {
					result.add(new AllocationSite(expression.getTypes(), ((AllocationSite) exp).getId()));
				} else if (exp instanceof HeapLocation) {
					result.add((HeapLocation) exp);
				}
			}

			return from(new PointBasedHeap(childState.elements, result));
		}

		if (expression instanceof HeapAllocation) {
			Set<AllocationSite> id = Collections.singleton(new AllocationSite(expression.getTypes(), pp.getLocation().getCodeLocation()));
			return from(new PointBasedHeap(id, cast(id)));
		}

		if (expression instanceof AllocationSite)
			return from(new PointBasedHeap(Collections.singleton((AllocationSite) expression), Collections.singleton((ValueExpression) expression)));
		
		if (expression instanceof ValueExpression)
			return from(new PointBasedHeap(Collections.emptySet(), Collections.singleton((ValueExpression) expression)));

		return top();
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, HeapEnvironment<PointBasedHeap> environment,
			ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public boolean tracksIdentifiers(Identifier id) {
		return id.getTypes().anyMatch(Type::isPointerType);
	}

	@Override
	protected PointBasedHeap mk(Set<AllocationSite> set) {
		return new PointBasedHeap(set, Collections.emptyList());
	}
}
