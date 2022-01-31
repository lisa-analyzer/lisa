package it.unive.lisa.analysis.nonInterference;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.inference.BaseInferredValue;
import it.unive.lisa.analysis.nonrelational.inference.InferenceSystem;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.annotations.matcher.AnnotationMatcher;
import it.unive.lisa.program.annotations.matcher.BasicAnnotationMatcher;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
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

	private static final Annotation LOW_CONF_ANNOTATION = new Annotation("lisa.ni.LowConfidentiality");

	private static final AnnotationMatcher LOW_CONF_MATCHER = new BasicAnnotationMatcher(LOW_CONF_ANNOTATION);

	private static final Annotation HIGH_INT_ANNOTATION = new Annotation("lisa.ni.HighIntegrity");

	private static final AnnotationMatcher HIGH_INT_MATCHER = new BasicAnnotationMatcher(HIGH_INT_ANNOTATION);

	private static final byte NI_BOTTOM = 0;

	private static final byte NI_LOW = 1;

	private static final byte NI_HIGH = 2;

	private final byte confidentiality;

	private final byte integrity;

	private final Map<ProgramPoint, NonInterference> guards;

	/**
	 * Builds a new instance of non interference, referring to the top element
	 * of the lattice.
	 */
	public NonInterference() {
		this(NI_HIGH, NI_LOW);
	}

	private NonInterference(byte confidentiality, byte integrity) {
		this.confidentiality = confidentiality;
		this.integrity = integrity;
		this.guards = new IdentityHashMap<>();
	}

	@Override
	public NonInterference top() {
		return new NonInterference(NI_HIGH, NI_LOW);
	}

	@Override
	public boolean isTop() {
		return confidentiality == NI_HIGH && integrity == NI_LOW;
	}

	@Override
	public NonInterference bottom() {
		return new NonInterference(NI_BOTTOM, NI_BOTTOM);
	}

	@Override
	public boolean isBottom() {
		return confidentiality == NI_BOTTOM && integrity == NI_BOTTOM;
	}

	/**
	 * Yields {@code true} if and only if this instance represents a
	 * {@code high} value for the confidentiality non interference analysis.
	 * 
	 * @return {@code true} if this is a high confidentiality element
	 */
	public boolean isHighConfidentiality() {
		return confidentiality == NI_HIGH;
	}

	/**
	 * Yields {@code true} if and only if this instance represents a {@code low}
	 * value for the confidentiality non interference analysis.
	 * 
	 * @return {@code true} if this is a low confidentiality element
	 */
	public boolean isLowConfidentiality() {
		return confidentiality == NI_LOW;
	}

	/**
	 * Yields {@code true} if and only if this instance represents a
	 * {@code high} value for the integrity non interference analysis.
	 * 
	 * @return {@code true} if this is a high integrity element
	 */
	public boolean isHighIntegrity() {
		return integrity == NI_HIGH;
	}

	/**
	 * Yields {@code true} if and only if this instance represents a {@code low}
	 * value for the integrity non interference analysis.
	 * 
	 * @return {@code true} if this is a low integrity element
	 */
	public boolean isLowIntegrity() {
		return integrity == NI_LOW;
	}

	@Override
	protected NonInterference lubAux(NonInterference other) throws SemanticException {
		// HL
		// | \
		// HH LL
		// | /
		// LH
		// |
		// BB
		byte confidentiality = isHighConfidentiality() || other.isHighConfidentiality() ? NI_HIGH : NI_LOW;
		byte integrity = isLowIntegrity() || other.isLowIntegrity() ? NI_LOW : NI_HIGH;
		return new NonInterference(confidentiality, integrity);
	}

	@Override
	protected NonInterference wideningAux(NonInterference other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected boolean lessOrEqualAux(NonInterference other) throws SemanticException {
		// HL
		// | \
		// HH LL
		// | /
		// LH
		// |
		// BB
		boolean confidentiality = isLowConfidentiality() || this.confidentiality == other.confidentiality;
		boolean integrity = isHighIntegrity() || this.integrity == other.integrity;
		return confidentiality && integrity;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + confidentiality;
		result = prime * result + ((guards == null) ? 0 : guards.hashCode());
		result = prime * result + integrity;
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
		if (confidentiality != other.confidentiality)
			return false;
		if (guards == null) {
			if (other.guards != null)
				return false;
		} else if (!guards.equals(other.guards))
			return false;
		if (integrity != other.integrity)
			return false;
		return true;
	}

	@Override
	public DomainRepresentation representation() {
		return isBottom() ? Lattice.BOTTOM_REPR
				: new StringRepresentation((isHighConfidentiality() ? "H" : "L") + (isHighIntegrity() ? "H" : "L"));
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

	private static NonInterference mkLowHigh() {
		return new NonInterference(NI_LOW, NI_HIGH);
	}

	private static NonInterference mkLowLow() {
		return new NonInterference(NI_LOW, NI_LOW);
	}

	private static NonInterference mkHighHigh() {
		return new NonInterference(NI_HIGH, NI_HIGH);
	}

	private NonInterference mkHighLow() {
		return top();
	}

	@Override
	protected InferredPair<NonInterference> evalNullConstant(NonInterference state, ProgramPoint pp)
			throws SemanticException {
		return new InferredPair<>(this, mkLowHigh(), state(state, pp));
	}

	@Override
	protected InferredPair<NonInterference> evalNonNullConstant(Constant constant, NonInterference state,
			ProgramPoint pp) throws SemanticException {
		return new InferredPair<>(this, mkLowHigh(), state(state, pp));
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
		Annotations annots = id.getAnnotations();
		if (annots.isEmpty())
			return mkHighLow();

		boolean lowConf = annots.contains(LOW_CONF_MATCHER);
		boolean highInt = annots.contains(HIGH_INT_MATCHER);

		if (lowConf && highInt)
			return mkLowHigh();
		else if (lowConf)
			return mkLowLow();
		else if (highInt)
			return mkHighHigh();
		else
			return mkHighLow();
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
