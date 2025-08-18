package it.unive.lisa.analysis.informationFlow;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.informationFlow.TaintLattice;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.annotations.matcher.AnnotationMatcher;
import it.unive.lisa.program.annotations.matcher.BasicAnnotationMatcher;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * A taint analysis, that is, an information-flow analysis tracking only
 * explicit flows. This domain uses annotations to mark variables as tainted
 * ({@link #TAINTED_ANNOTATION}) or clean ({@link #CLEAN_ANNOTATION}).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the concrete type of the lattice elements tracked by the analysis
 */
public abstract class BaseTaint<L extends TaintLattice<L>> implements BaseNonRelationalValueDomain<L> {

	/**
	 * The annotation used to mark tainted variables, that is, sources of
	 * tainted information.
	 */
	public static final Annotation TAINTED_ANNOTATION = new Annotation("lisa.taint.Tainted");

	/**
	 * An {@link AnnotationMatcher} for {@link #TAINTED_ANNOTATION}.
	 */
	public static final AnnotationMatcher TAINTED_MATCHER = new BasicAnnotationMatcher(TAINTED_ANNOTATION);

	/**
	 * The annotation used to mark clean variables, that is, sanitizers of
	 * tainted information.
	 */
	public static final Annotation CLEAN_ANNOTATION = new Annotation("lisa.taint.Clean");

	/**
	 * An {@link AnnotationMatcher} for {@link #CLEAN_ANNOTATION}.
	 */
	public static final AnnotationMatcher CLEAN_MATCHER = new BasicAnnotationMatcher(CLEAN_ANNOTATION);

	/**
	 * Default approximation for {@link Identifier}s. This method returns the
	 * same as
	 * {@link BaseNonRelationalValueDomain#fixedVariable(Identifier, ProgramPoint, SemanticOracle)}
	 * if the given identifier has no annotations. Otherwise, it relies on the
	 * presence if {@link #TAINTED_ANNOTATION} and {@link #CLEAN_ANNOTATION} to
	 * produce abstract values. defaulting to bottom. <br>
	 * <br>
	 * If this method does not return bottom, it is used as return value for
	 * both {@link #fixedVariable(Identifier, ProgramPoint, SemanticOracle)} and
	 * {@link #evalIdentifier(Identifier, ValueEnvironment, ProgramPoint, SemanticOracle)}.
	 * 
	 * @param id     the identifier to evaluate
	 * @param pp     the program point where the evaluation happens
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return a fixed approximation for the given variable, if any
	 * 
	 * @throws SemanticException if an exception happens during the evaluation
	 */
	protected L defaultApprox(
			Identifier id,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Annotations annots = id.getAnnotations();
		if (annots.isEmpty())
			return BaseNonRelationalValueDomain.super.fixedVariable(id, pp, oracle);

		if (annots.contains(BaseTaint.TAINTED_MATCHER))
			return tainted();

		if (annots.contains(BaseTaint.CLEAN_MATCHER))
			return clean();

		return bottom();
	}

	@Override
	public L fixedVariable(
			Identifier id,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		L def = defaultApprox(id, pp, oracle);
		if (!def.isBottom())
			return def;
		return BaseNonRelationalValueDomain.super.fixedVariable(id, pp, oracle);
	}

	@Override
	public L evalIdentifier(
			Identifier id,
			ValueEnvironment<L> environment,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		L def = defaultApprox(id, pp, oracle);
		if (!def.isBottom())
			return def;
		return BaseNonRelationalValueDomain.super.evalIdentifier(id, environment, pp, oracle);
	}

	@Override
	public L evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return top();
	}

	@Override
	public L evalNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return clean();
	}

	@Override
	public L evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return clean();
	}

	@Override
	public L evalUnaryExpression(
			UnaryExpression expression,
			L arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return arg;
	}

	@Override
	public L evalBinaryExpression(
			BinaryExpression expression,
			L left,
			L right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return left.or(right);
	}

	@Override
	public L evalTernaryExpression(
			TernaryExpression expression,
			L left,
			L middle,
			L right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return left.or(middle).or(right);
	}

	@Override
	public L evalTypeCast(
			BinaryExpression cast,
			L left,
			L right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return left;
	}

	@Override
	public L evalTypeConv(
			BinaryExpression conv,
			L left,
			L right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return left;
	}

	@Override
	public Satisfiability satisfies(
			ValueEnvironment<L> environment,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// quick answer: we cannot do anything
		return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<L> assume(
			ValueEnvironment<L> environment,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		// quick answer: we cannot do anything
		return environment;
	}

	/**
	 * Yields the domain element that represents tainted values.
	 * 
	 * @return the tainted domain element
	 */
	protected abstract L tainted();

	/**
	 * Yields the domain element that represents clean values.
	 * 
	 * @return the clean domain element
	 */
	protected abstract L clean();

}
