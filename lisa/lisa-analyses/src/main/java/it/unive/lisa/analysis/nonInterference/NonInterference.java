package it.unive.lisa.analysis.nonInterference;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.GenericMapLattice;
import it.unive.lisa.analysis.nonrelational.BaseNonRelationalDomain;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.analysis.nonrelational.NonRelationalDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.annotations.matcher.AnnotationMatcher;
import it.unive.lisa.program.annotations.matcher.BasicAnnotationMatcher;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.ObjectRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * Implementation of the non interference analysis as an {@link Environment}
 * containing instances of {@link NI}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @see <a href=
 *          "https://en.wikipedia.org/wiki/Non-interference_(security)">Non-interference</a>
 */
public class NonInterference
		extends
		Environment<NonInterference, ValueExpression, NonInterference.NI>
		implements
		ValueDomain<NonInterference> {

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

	private GenericMapLattice<ProgramPoint, NI> guards;

	/**
	 * Builds the top instance of the non-interference domain.
	 */
	public NonInterference() {
		super(new NI());
		this.guards = new GenericMapLattice<ProgramPoint, NonInterference.NI>(NI.mkLowHigh());
	}

	private NonInterference(
			NI domain,
			Map<Identifier, NI> function,
			GenericMapLattice<ProgramPoint, NI> guards) {
		super(domain, function);
		this.guards = guards;
	}

	@Override
	public NonInterference top() {
		return isTop() ? this : new NonInterference(lattice.top(), null, guards.top());
	}

	@Override
	public boolean isTop() {
		return super.isTop() && guards.isTop();
	}

	@Override
	public NonInterference bottom() {
		return isBottom() ? this : new NonInterference(lattice.bottom(), null, guards.bottom());
	}

	@Override
	public boolean isBottom() {
		return super.isBottom() && guards.isBottom();
	}

	@Override
	public NonInterference mk(
			NI lattice,
			Map<Identifier, NI> function) {
		return new NonInterference(lattice, function, guards);
	}

	/**
	 * Yields the state in which the execution is. This corresponds to the lub
	 * of the {@link NI} instances of all the guards protecting the program
	 * point where this object was created.
	 * 
	 * @return the execution state
	 */
	public NI getExecutionState() {
		if (guards.function == null || guards.function.isEmpty())
			// we return LH since that is the lowest non-error state
			return NI.mkLowHigh();

		// we start at LH since that is the lowest non-error state
		try {
			NI res = NI.mkLowHigh();
			for (Entry<ProgramPoint, NI> guard : guards)
				res = res.lub(guard.getValue());
			return res;
		} catch (SemanticException e) {
			return lattice.bottom();
		}
	}

	@Override
	public boolean lessOrEqualAux(
			NonInterference other)
			throws SemanticException {
		return super.lessOrEqualAux(other) && guards.lessOrEqual(other.guards);
	}

	@Override
	public NonInterference lubAux(
			NonInterference other)
			throws SemanticException {
		NonInterference lub = super.lubAux(other);
		return new NonInterference(lub.lattice, lub.function, guards.lub(other.guards));
	}

	@Override
	public NonInterference glbAux(
			NonInterference other)
			throws SemanticException {
		NonInterference glb = super.glbAux(other);
		return new NonInterference(glb.lattice, glb.function, guards.glb(other.guards));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(guards);
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		NonInterference other = (NonInterference) obj;
		return Objects.equals(guards, other.guards);
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom() || isTop())
			return super.representation();

		return new ObjectRepresentation(
				Map.of("map", super.representation(), "state", getExecutionState().representation()));
	}

	@Override
	public NonInterference assign(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		NonInterference assign = super.assign(id, expression, pp, oracle);
		Collection<Statement> guards = pp.getCFG().getGuards(pp);
		GenericMapLattice<ProgramPoint, NI> newGuards = assign.guards.transform(
				k -> guards.contains(k) ? k : null,
				v -> v,
				Lattice::lub);
		return new NonInterference(assign.lattice, assign.function, newGuards);
	}

	@Override
	public NonInterference smallStepSemantics(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		NonInterference sss = super.smallStepSemantics(expression, pp, oracle);
		Collection<Statement> guards = pp.getCFG().getGuards(pp);
		GenericMapLattice<ProgramPoint, NI> newGuards = sss.guards.transform(
				k -> guards.contains(k) ? k : null,
				v -> v,
				Lattice::lub);
		return new NonInterference(sss.lattice, sss.function, newGuards);
	}

	/**
	 * A pair of integrity and confidentiality bits for the
	 * {@link NonInterference} analysis. This class is made to be a
	 * {@link NonRelationalDomain} just to be used in conjunction with
	 * {@link NonInterference}, which is an {@link Environment}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class NI
			implements
			BaseNonRelationalDomain<NI, NonInterference> {

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

		/**
		 * Builds a new instance of non interference, referring to the top
		 * element of the lattice.
		 */
		public NI() {
			this(NI_HIGH, NI_LOW);
		}

		/**
		 * Builds the abstract value for the given confidentiality and integrity
		 * values. Each of those can be either 0 for bottom
		 * ({@link #NI_BOTTOM}), 1 for low ({@link #NI_LOW}), or 2 for high
		 * ({@link #NI_HIGH}).
		 * 
		 * @param confidentiality the confidentiality value
		 * @param integrity       the integrity value
		 */
		public NI(
				byte confidentiality,
				byte integrity) {
			this.confidentiality = confidentiality;
			this.integrity = integrity;
		}

		@Override
		public NI top() {
			return new NI(NI_HIGH, NI_LOW);
		}

		@Override
		public boolean isTop() {
			return confidentiality == NI_HIGH && integrity == NI_LOW;
		}

		@Override
		public NI bottom() {
			return new NI(NI_BOTTOM, NI_BOTTOM);
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
		 * Yields {@code true} if and only if this instance represents a
		 * {@code low} value for the confidentiality non interference analysis.
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
		 * Yields {@code true} if and only if this instance represents a
		 * {@code low} value for the integrity non interference analysis.
		 * 
		 * @return {@code true} if this is a low integrity element
		 */
		public boolean isLowIntegrity() {
			return integrity == NI_LOW;
		}

		@Override
		public NI lubAux(
				NI other)
				throws SemanticException {
			// HL
			// | \
			// HH LL
			// | /
			// LH
			// |
			// BB
			byte confidentiality = isHighConfidentiality() || other.isHighConfidentiality() ? NI_HIGH : NI_LOW;
			byte integrity = isLowIntegrity() || other.isLowIntegrity() ? NI_LOW : NI_HIGH;
			return new NI(confidentiality, integrity);
		}

		@Override
		public boolean lessOrEqualAux(
				NI other)
				throws SemanticException {
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
			result = prime * result + integrity;
			return result;
		}

		@Override
		public boolean equals(
				Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NI other = (NI) obj;
			if (confidentiality != other.confidentiality)
				return false;
			if (integrity != other.integrity)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return representation().toString();
		}

		@Override
		public StructuredRepresentation representation() {
			if (isBottom())
				return Lattice.bottomRepresentation();
			return new StringRepresentation((isHighConfidentiality() ? "H" : "L") + (isHighIntegrity() ? "H" : "L"));
		}

		private static NI mkLowHigh() {
			return new NI(NI_LOW, NI_HIGH);
		}

		private static NI mkLowLow() {
			return new NI(NI_LOW, NI_LOW);
		}

		private static NI mkHighHigh() {
			return new NI(NI_HIGH, NI_HIGH);
		}

		private NI mkHighLow() {
			return top();
		}

		@Override
		public NI evalTypeConv(
				BinaryExpression conv,
				NI left,
				NI right,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return left;
		}

		@Override
		public NI evalTypeCast(
				BinaryExpression cast,
				NI left,
				NI right,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return left;
		}

		@Override
		public NI evalNullConstant(
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return mkLowHigh();
		}

		@Override
		public NI evalNonNullConstant(
				Constant constant,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return mkLowHigh();
		}

		@Override
		public NI evalUnaryExpression(
				UnaryExpression expression,
				NI arg,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return arg;
		}

		@Override
		public NI evalBinaryExpression(
				BinaryExpression expression,
				NI left,
				NI right,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return left.lub(right);
		}

		@Override
		public NI evalTernaryExpression(
				TernaryExpression expression,
				NI left,
				NI middle,
				NI right,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return left.lub(middle).lub(right);
		}

		@Override
		public NI evalIdentifier(
				Identifier id,
				NonInterference environment,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return fixedVariable(id, pp, oracle);
		}

		@Override
		public NI fixedVariable(
				Identifier id,
				ProgramPoint pp,
				SemanticOracle oracle) {
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
		public NonInterference assume(
				NonInterference environment,
				ValueExpression expression,
				ProgramPoint src,
				ProgramPoint dest,
				SemanticOracle oracle)
				throws SemanticException {
			NI eval = eval(expression, environment, src, oracle);
			GenericMapLattice<ProgramPoint, NI> guards = environment.guards.putState(src, eval);
			return new NonInterference(environment.lattice, environment.function, guards);
		}
	}
}
