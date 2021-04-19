package it.unive.lisa.analysis.impl.nonInterference;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.inference.BaseInferredValue;
import it.unive.lisa.analysis.inference.InferenceSystem;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryOperator;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The type-system based implementation of the non interference analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @see <a href=
 *          "https://en.wikipedia.org/wiki/Non-interference_(security)">Non-interference</a>
 */
public class NonInterference extends BaseInferredValue<NonInterference> {

	private final Boolean ni;

	private final Map<ProgramPoint, NonInterference> guards;

	/**
	 * Builds a new instance of non interference, referring to the top element
	 * of the lattice.
	 */
	public NonInterference() {
		this(true);
	}

	private NonInterference(Boolean ni) {
		this.ni = ni;
		this.guards = new IdentityHashMap<>();
	}

	@Override
	public NonInterference top() {
		return new NonInterference(true);
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * For non-interference, the top value is the HIGH element, so invoking this
	 * method or {@link #isHigh()} yields the same result.
	 */
	@Override
	public boolean isTop() {
		return ni != null && ni == true;
	}

	@Override
	public NonInterference bottom() {
		return new NonInterference(null);
	}

	@Override
	public boolean isBottom() {
		return ni == null;
	}

	/**
	 * Yields {@code true} if and only if this instance represents a
	 * {@code high} value for the non interference analysis.
	 * 
	 * @return {@code true} if this is the high element
	 */
	public boolean isHigh() {
		return isTop();
	}

	/**
	 * Yields {@code true} if and only if this instance represents a {@code low}
	 * value for the non interference analysis.
	 * 
	 * @return {@code true} if this is the low element
	 */
	public boolean isLow() {
		return ni != null && ni == false;
	}

	@Override
	protected NonInterference lubAux(NonInterference other) throws SemanticException {
		// never called -- three-elements lattice
		return top();
	}

	@Override
	protected NonInterference wideningAux(NonInterference other) throws SemanticException {
		// never called -- three-elements lattice
		return top();
	}

	@Override
	protected boolean lessOrEqualAux(NonInterference other) throws SemanticException {
		// never called -- three-elements lattice
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((guards == null) ? 0 : guards.hashCode());
		result = prime * result + ((ni == null) ? 0 : ni.hashCode());
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
		NonInterference other = (NonInterference) obj;
		if (guards == null) {
			if (other.guards != null)
				return false;
		} else if (!guards.equals(other.guards))
			return false;
		if (ni == null) {
			if (other.ni != null)
				return false;
		} else if (!ni.equals(other.ni))
			return false;
		return true;
	}

	@Override
	public String representation() {
		return isBottom() ? Lattice.BOTTOM_STRING : isHigh() ? "H" : "L";
	}

	private NonInterference state(NonInterference state, ProgramPoint pp) throws SemanticException {
		Map<ProgramPoint, NonInterference> guards = new IdentityHashMap<>();
		for (ProgramPoint guard : pp.getCFG().getGuards(pp))
			guards.put(guard, state.guards.getOrDefault(guard, bottom()));
		NonInterference res = bottom();
		for (NonInterference guard : guards.values())
			res = res.lub(guard);

		// we have to create a new one here, otherwise we would end up
		// adding those entries to one of the
		guards.forEach(res.guards::put);
		return res;
	}

	private NonInterference mkLow() {
		return new NonInterference(false);
	}

	private NonInterference mkHigh() {
		return top();
	}

	@Override
	protected InferredPair<NonInterference> evalNullConstant(NonInterference state, ProgramPoint pp)
			throws SemanticException {
		return new InferredPair<>(this, mkLow(), state(state, pp));
	}

	@Override
	protected InferredPair<NonInterference> evalNonNullConstant(Constant constant, NonInterference state,
			ProgramPoint pp) throws SemanticException {
		return new InferredPair<>(this, mkLow(), state(state, pp));
	}

	@Override
	protected InferredPair<NonInterference> evalUnaryExpression(UnaryOperator operator, NonInterference arg,
			NonInterference state, ProgramPoint pp) throws SemanticException {
		return new InferredPair<>(this, arg, state(state, pp));
	}

	@Override
	protected InferredPair<NonInterference> evalBinaryExpression(BinaryOperator operator, NonInterference left,
			NonInterference right, NonInterference state, ProgramPoint pp) throws SemanticException {
		return new InferredPair<>(this, left.lub(right), state(state, pp));
	}

	@Override
	protected InferredPair<NonInterference> evalTernaryExpression(TernaryOperator operator, NonInterference left,
			NonInterference middle, NonInterference right, NonInterference state, ProgramPoint pp)
			throws SemanticException {
		return new InferredPair<>(this, left.lub(middle).lub(right), state(state, pp));
	}

	@Override
	protected InferredPair<NonInterference> evalIdentifier(Identifier id,
			InferenceSystem<NonInterference> environment, ProgramPoint pp) throws SemanticException {
		return new InferredPair<>(this, variable(id, null), state(environment.getExecutionState(), pp));
	}

	@Override
	public NonInterference variable(Identifier id, ProgramPoint pp) {
		return id.getName().startsWith("L_") ? mkLow() : mkHigh();
	}

	@Override
	public boolean tracksIdentifiers(Identifier id) {
		return true;
	}

	@Override
	public boolean canProcess(SymbolicExpression expression) {
		return !expression.getDynamicType().isPointerType();
	}

	@Override
	public InferenceSystem<NonInterference> assume(InferenceSystem<NonInterference> environment,
			ValueExpression expression, ProgramPoint pp) throws SemanticException {
		InferredPair<NonInterference> eval = eval(expression, environment, pp);
		NonInterference inf = eval.getInferred();
		eval.getState().guards.forEach(inf.guards::put);
		inf.guards.put(pp, inf);
		return new InferenceSystem<>(environment, inf);
	}
}
