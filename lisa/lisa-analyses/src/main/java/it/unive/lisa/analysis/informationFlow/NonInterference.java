package it.unive.lisa.analysis.informationFlow;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.GenericMapLattice;
import it.unive.lisa.analysis.nonrelational.BaseNonRelationalDomain;
import it.unive.lisa.analysis.value.ValueDomain;
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
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Collection;

/**
 * Implementation of the non interference analysis, using annotations to detect
 * low confidentiality variables/fields/functions ({@link LOW_CONF_ANNOTATION})
 * and high integrity variables/fields/functions ({@link HIGH_INT_ANNOTATION}).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
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

}
