package it.unive.lisa.analysis.heap;

import it.unive.lisa.analysis.BaseHeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.HeapIdentifier;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;

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

	private final Collection<ValueExpression> rewritten;

	/**
	 * Builds a new instance. Invoking {@link #getRewrittenExpressions()} on
	 * this instance will return a singleton set containing one {@link Skip}.
	 */
	public MonolithicHeap() {
		this(new Skip());
	}

	private MonolithicHeap(ValueExpression rewritten) {
		this(Collections.singleton(rewritten));
	}

	private MonolithicHeap(Collection<ValueExpression> rewritten) {
		this.rewritten = rewritten;
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
	protected MonolithicHeap semanticsOf(HeapExpression expression) {
		// any expression accessing an area of the heap or instantiating a new
		// one is modeled through the monolith
		return new MonolithicHeap(new HeapIdentifier(expression.getTypes(), MONOLITH_NAME, true));
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
	public MonolithicHeap forgetIdentifier(Identifier id) throws SemanticException {
		return new MonolithicHeap(rewritten);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected MonolithicHeap lubAux(MonolithicHeap other) throws SemanticException {
		return new MonolithicHeap(CollectionUtils.union(rewritten, other.rewritten));
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
	public String representation() {
		return "monolith";
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
