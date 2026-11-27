package it.unive.lisa.analysis.informationFlow;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.constraints.WholeValueAnalysis;
import it.unive.lisa.analysis.nonrelational.BaseNonRelationalDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.GenericMapLattice;
import it.unive.lisa.lattices.informationFlow.NonInterferenceEnvironment;
import it.unive.lisa.lattices.informationFlow.NonInterferenceValue;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.annotations.matcher.AnnotationMatcher;
import it.unive.lisa.program.annotations.matcher.BasicAnnotationMatcher;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import java.util.Collection;
import java.util.Set;

/**
 * Implementation of the non-interference analysis, an information flow analysis
 * checking that variations of high-confidentiality (secret) or low-integrity
 * (untrusted) inputs never affect the low-confidentiality (public) or
 * high-integrity (trusted) observable behavior of a program. Non-interference
 * is checked here through a security lattice with four elements, combining
 * confidentiality (low/high) and integrity (low/high), tracked independently
 * for each expression and, through {@link NonInterferenceEnvironment#guards},
 * for each guard that is currently open in the control flow, so that implicit
 * flows caused by branching on secret or untrusted data are detected as well.
 * Annotations are used to mark variables, fields and functions as low
 * confidentiality ({@link #LOW_CONF_ANNOTATION}) or high integrity
 * ({@link #HIGH_INT_ANNOTATION}). <br/>
 * <br/>
 * As an information flow analysis, this domain does not take part in
 * {@link WholeValueAnalysis}, meaning that it will never generate constraints
 * when asked to.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @see <a href="https://doi.org/10.1109/SP.1982.10014">Joseph A. Goguen, José
 *          Meseguer. Security Policies and Security Models. In 1982 IEEE
 *          Symposium on Security and Privacy, pages 11-20, IEEE Computer
 *          Society, 1982.</a>
 * @see <a href=
 *          "https://en.wikipedia.org/wiki/Non-interference_(security)">Non-interference</a>
 */
public class NonInterference
		implements
		BaseNonRelationalDomain<NonInterferenceValue, NonInterferenceEnvironment>,
		ValueDomain<NonInterferenceEnvironment> {

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

	@Override
	public NonInterferenceEnvironment makeLattice() {
		return new NonInterferenceEnvironment();
	}

	@Override
	public NonInterferenceEnvironment assign(
			NonInterferenceEnvironment state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		NonInterferenceEnvironment assign = BaseNonRelationalDomain.super.assign(state, id, expression, pp, oracle);
		Collection<Statement> guards = pp.getCFG().getGuards(pp);
		GenericMapLattice<ProgramPoint, NonInterferenceValue> newGuards = assign.guards
				.transform(k -> guards.contains(k) ? k : null, v -> v, Lattice::lub);
		return new NonInterferenceEnvironment(assign.lattice, assign.function, newGuards);
	}

	@Override
	public NonInterferenceEnvironment smallStepSemantics(
			NonInterferenceEnvironment state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		NonInterferenceEnvironment sss = BaseNonRelationalDomain.super.smallStepSemantics(
				state,
				expression,
				pp,
				oracle);
		Collection<Statement> guards = pp.getCFG().getGuards(pp);
		GenericMapLattice<ProgramPoint, NonInterferenceValue> newGuards = sss.guards
				.transform(k -> guards.contains(k) ? k : null, v -> v, Lattice::lub);
		return new NonInterferenceEnvironment(sss.lattice, sss.function, newGuards);
	}

	@Override
	public NonInterferenceValue evalTypeConv(
			BinaryExpression conv,
			NonInterferenceValue left,
			NonInterferenceValue right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return left;
	}

	@Override
	public NonInterferenceValue evalTypeCast(
			BinaryExpression cast,
			NonInterferenceValue left,
			NonInterferenceValue right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return left;
	}

	@Override
	public NonInterferenceValue evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return NonInterferenceValue.LOW_HIGH;
	}

	@Override
	public NonInterferenceValue evalUnaryExpression(
			UnaryExpression expression,
			NonInterferenceValue arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return arg;
	}

	@Override
	public NonInterferenceValue evalBinaryExpression(
			BinaryExpression expression,
			NonInterferenceValue left,
			NonInterferenceValue right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return left.lub(right);
	}

	@Override
	public NonInterferenceValue evalTernaryExpression(
			TernaryExpression expression,
			NonInterferenceValue left,
			NonInterferenceValue middle,
			NonInterferenceValue right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return left.lub(middle).lub(right);
	}

	@Override
	public NonInterferenceValue evalIdentifier(
			Identifier id,
			NonInterferenceEnvironment environment,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return fixedVariable(id, pp, oracle);
	}

	@Override
	public NonInterferenceValue fixedVariable(
			Identifier id,
			ProgramPoint pp,
			SemanticOracle oracle) {
		Annotations annots = id.getAnnotations();
		if (annots.isEmpty())
			return NonInterferenceValue.HIGH_LOW;

		boolean lowConf = annots.contains(LOW_CONF_MATCHER);
		boolean highInt = annots.contains(HIGH_INT_MATCHER);

		if (lowConf && highInt)
			return NonInterferenceValue.LOW_HIGH;
		else if (lowConf)
			return NonInterferenceValue.LOW_LOW;
		else if (highInt)
			return NonInterferenceValue.HIGH_HIGH;
		else
			return NonInterferenceValue.HIGH_LOW;
	}

	@Override
	public NonInterferenceEnvironment assume(
			NonInterferenceEnvironment environment,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		NonInterferenceValue eval = eval(environment, expression, src, oracle);
		GenericMapLattice<ProgramPoint, NonInterferenceValue> guards = environment.guards.putState(src, eval);
		return new NonInterferenceEnvironment(environment.lattice, environment.function, guards);
	}

	@Override
	public NonInterferenceValue top() {
		return NonInterferenceValue.HIGH_LOW;
	}

	@Override
	public NonInterferenceValue bottom() {
		return NonInterferenceValue.BOTTOM;
	}

	@Override
	public NonInterferenceEnvironment onCallReturn(
			NonInterferenceEnvironment entryState,
			NonInterferenceEnvironment callres,
			ProgramPoint call)
			throws SemanticException {
		// the non-interference level goes back to the one
		// of the caller after it returns
		if (entryState.guards == null)
			if (callres.guards == null)
				return callres;
			else
				return new NonInterferenceEnvironment(
						callres.lattice,
						callres.function,
						entryState.guards);
		else if (entryState.guards.equals(callres.guards))
			return callres;
		else
			return new NonInterferenceEnvironment(
					callres.lattice,
					callres.function,
					entryState.guards);
	}

	@Override
	public boolean canProcess(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression instanceof PushInv)
			// the type approximation of a pushinv is bottom, so the below check
			// will always fail regardless of the kind of value we are tracking
			return expression.getStaticType().isValueType();

		Set<Type> rts = null;
		try {
			rts = oracle.getRuntimeTypesOf(expression, pp);
		} catch (SemanticException e) {
			return false;
		}

		if (rts == null || rts.isEmpty())
			// if we have no runtime types, either the type domain has no type
			// information for the given expression (thus it can be anything,
			// also something that we can track) or the computation returned
			// bottom (and the whole state is likely going to go to bottom
			// anyway).
			return true;

		return rts.stream().anyMatch(Type::isValueType);
	}
}
