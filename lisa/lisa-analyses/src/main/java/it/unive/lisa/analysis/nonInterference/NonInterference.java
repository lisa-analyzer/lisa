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

	/**
	 * The annotation used to mark low confidentiality variables.
	 */
	public static final Annotation LOW_CONF_ANNOTATION = new Annotation("lisa.ni.LowConfidentiality");

	/**
	 * {@link AnnotationMatcher} for {@link #LOW_CONF_ANNOTATION}.
	 */
	public static final AnnotationMatcher LOW_CONF_MATCHER = new BasicAnnotationMatcher(LOW_CONF_ANNOTATION);

	/**
	 * The annotation used to mark high integrity variables.
	 */
	public static final Annotation HIGH_INT_ANNOTATION = new Annotation("lisa.ni.HighIntegrity");

	/**
	 * {@link AnnotationMatcher} for {@link #HIGH_INT_ANNOTATION}.
	 */
	public static final AnnotationMatcher HIGH_INT_MATCHER = new BasicAnnotationMatcher(HIGH_INT_ANNOTATION);

	/**
	 * The value to use for bottom non interference levels.
	 */
	public static final byte NI_BOTTOM = 0;

	/**
	 * The value to use for low non interference levels.
	 */
	public static final byte NI_LOW = 1;

	/**
	 * The value to use for high non interference levels.
	 */
	public static final byte NI_HIGH = 2;

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

	/**
	 * Builds the abstract value for the given confidentiality and integrity
	 * values. Each of those can be either 0 for bottom ({@link #NI_BOTTOM}), 1
	 * for low ({@link #NI_LOW}), or 2 for high ({@link #NI_HIGH}).
	 * 
	 * @param confidentiality the confidentiality value
	 * @param integrity       the integrity value
	 */
	public NonInterference(byte confidentiality, byte integrity) {
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
	public NonInterference lubAux(NonInterference other) throws SemanticException {
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
	public boolean lessOrEqualAux(NonInterference other) throws SemanticException {
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
		if (isBottom())
			return Lattice.bottomRepresentation();
		return new StringRepresentation((isHighConfidentiality() ? "H" : "L") + (isHighIntegrity() ? "H" : "L"));
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
	public InferredPair<NonInterference> evalNullConstant(NonInterference state, ProgramPoint pp)
			throws SemanticException {
		return new InferredPair<>(this, mkLowHigh(), state(state, pp));
	}

	@Override
	public InferredPair<NonInterference> evalNonNullConstant(Constant constant, NonInterference state,
			ProgramPoint pp) throws SemanticException {
		return new InferredPair<>(this, mkLowHigh(), state(state, pp));
	}

	@Override
	public InferredPair<NonInterference> evalUnaryExpression(UnaryOperator operator, NonInterference arg,
			NonInterference state, ProgramPoint pp) throws SemanticException {
		return new InferredPair<>(this, arg, state(state, pp));
	}

	@Override
	public InferredPair<NonInterference> evalBinaryExpression(BinaryOperator operator, NonInterference left,
			NonInterference right, NonInterference state, ProgramPoint pp) throws SemanticException {
		return new InferredPair<>(this, left.lub(right), state(state, pp));
	}

	@Override
	public InferredPair<NonInterference> evalTernaryExpression(TernaryOperator operator, NonInterference left,
			NonInterference middle, NonInterference right, NonInterference state, ProgramPoint pp)
			throws SemanticException {
		return new InferredPair<>(this, left.lub(middle).lub(right), state(state, pp));
	}

	@Override
	public InferredPair<NonInterference> evalIdentifier(Identifier id,
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
	public InferenceSystem<NonInterference> assume(InferenceSystem<NonInterference> environment,
			ValueExpression expression, ProgramPoint pp) throws SemanticException {
		InferredPair<NonInterference> eval = eval(expression, environment, pp);
		NonInterference inf = eval.getInferred();
		eval.getState().guards.forEach(inf.guards::put);
		inf.guards.put(pp, inf);
		return new InferenceSystem<>(environment, inf);
	}
}
