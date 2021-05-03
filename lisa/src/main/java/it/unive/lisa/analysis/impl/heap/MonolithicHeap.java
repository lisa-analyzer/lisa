package it.unive.lisa.analysis.impl.heap;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.BaseHeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Collections;
import java.util.List;

/**
 * A monolithic heap implementation that abstracts all heap locations to a
 * unique identifier.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class MonolithicHeap extends BaseHeapDomain<MonolithicHeap> {

	private static final MonolithicHeap TOP = new MonolithicHeap();

	private static final MonolithicHeap BOTTOM = new MonolithicHeap();

	private static final String MONOLITH_NAME = "heap";

	private static final DomainRepresentation REPR = new StringRepresentation("monolith");

	private final ExpressionSet<ValueExpression> rewritten;

	/**
	 * Builds a new instance. Invoking {@link #getRewrittenExpressions()} on
	 * this instance will return a singleton set containing one {@link Skip}.
	 */
	public MonolithicHeap() {
		this(new Skip());
	}

	private MonolithicHeap(ValueExpression rewritten) {
		this(new ExpressionSet<ValueExpression>(rewritten));
	}

	private MonolithicHeap(ExpressionSet<ValueExpression> rewritten) {
		this.rewritten = rewritten;
	}

	@Override
	public ExpressionSet<ValueExpression> getRewrittenExpressions() {
		return rewritten;
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return Collections.emptyList();
	}

	@Override
	public MonolithicHeap assign(Identifier id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		// the only thing that we do is rewrite the expression if needed
		return smallStepSemantics(expression, pp);
	}

	@Override
	protected MonolithicHeap mk(MonolithicHeap reference, ValueExpression expression) {
		return new MonolithicHeap(expression);
	}

	@Override
	protected MonolithicHeap semanticsOf(HeapExpression expression, ProgramPoint pp) {
		// any expression accessing an area of the heap or instantiating a new
		// one is modeled through the monolith
		return new MonolithicHeap(new HeapLocation(expression.getTypes(), MONOLITH_NAME, true));
	}

	@Override
	public MonolithicHeap assume(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		// the only thing that we do is rewrite the expression if needed
		return smallStepSemantics(expression, pp);
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		// we leave the decision to the value domain
		return Satisfiability.UNKNOWN;
	}

	@Override
	public MonolithicHeap pushScope(ScopeToken scope) throws SemanticException {
		return top();
	}

	@Override
	public MonolithicHeap popScope(ScopeToken scope) throws SemanticException {
		return top();
	}

	@Override
	public MonolithicHeap forgetIdentifier(Identifier id) throws SemanticException {
		return new MonolithicHeap(rewritten);
	}

	@Override
	protected MonolithicHeap lubAux(MonolithicHeap other) throws SemanticException {
		return new MonolithicHeap(rewritten.lub(other.rewritten));
	}

	@Override
	protected MonolithicHeap wideningAux(MonolithicHeap other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected boolean lessOrEqualAux(MonolithicHeap other) throws SemanticException {
		return true;
	}

	@Override
	public MonolithicHeap top() {
		return TOP;
	}

	@Override
	public MonolithicHeap bottom() {
		return BOTTOM;
	}

	@Override
	public DomainRepresentation representation() {
		return REPR;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		MonolithicHeap other = (MonolithicHeap) obj;
		if (rewritten == null) {
			if (other.rewritten != null)
				return false;
		} else if (!rewritten.equals(other.rewritten))
			return false;
		return true;
	}
}
