package it.unive.lisa.analysis.nonInterference;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.GenericMapLattice;
import it.unive.lisa.analysis.nonInterference.NIEnv.NI;
import it.unive.lisa.analysis.nonrelational.BaseNonRelationalDomain;
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
		BaseNonRelationalDomain<NIEnv.NI, NIEnv>,
		ValueDomain<NIEnv> {

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
	public NIEnv makeLattice() {
		return new NIEnv();
	}

	@Override
	public NIEnv assign(
			NIEnv state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		NIEnv assign = BaseNonRelationalDomain.super.assign(state, id, expression, pp, oracle);
		Collection<Statement> guards = pp.getCFG().getGuards(pp);
		GenericMapLattice<ProgramPoint,
				NI> newGuards = assign.guards.transform(k -> guards.contains(k) ? k : null, v -> v, Lattice::lub);
		return new NIEnv(assign.lattice, assign.function, newGuards);
	}

	@Override
	public NIEnv smallStepSemantics(
			NIEnv state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		NIEnv sss = BaseNonRelationalDomain.super.smallStepSemantics(state, expression, pp, oracle);
		Collection<Statement> guards = pp.getCFG().getGuards(pp);
		GenericMapLattice<ProgramPoint,
				NI> newGuards = sss.guards.transform(k -> guards.contains(k) ? k : null, v -> v, Lattice::lub);
		return new NIEnv(sss.lattice, sss.function, newGuards);
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
		return NI.LOW_HIGH;
	}

	@Override
	public NI evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return NI.LOW_HIGH;
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
			NIEnv environment,
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
			return NI.HIGH_LOW;

		boolean lowConf = annots.contains(LOW_CONF_MATCHER);
		boolean highInt = annots.contains(HIGH_INT_MATCHER);

		if (lowConf && highInt)
			return NI.LOW_HIGH;
		else if (lowConf)
			return NI.LOW_LOW;
		else if (highInt)
			return NI.HIGH_HIGH;
		else
			return NI.HIGH_LOW;
	}

	@Override
	public NIEnv assume(
			NIEnv environment,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		NI eval = eval(environment, expression, src, oracle);
		GenericMapLattice<ProgramPoint, NI> guards = environment.guards.putState(src, eval);
		return new NIEnv(environment.lattice, environment.function, guards);
	}

	@Override
	public NI top() {
		return NI.HIGH_LOW;
	}

	@Override
	public NI bottom() {
		return NI.BOTTOM;
	}

}
