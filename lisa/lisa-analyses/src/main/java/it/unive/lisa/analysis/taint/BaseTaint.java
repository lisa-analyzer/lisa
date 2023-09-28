package it.unive.lisa.analysis.taint;

import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.annotations.matcher.AnnotationMatcher;
import it.unive.lisa.program.annotations.matcher.BasicAnnotationMatcher;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;

/**
 * A taint analysis, that is, an information-flow analysis tracking only
 * explicit flows.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of the analysis
 */
public abstract class BaseTaint<T extends BaseTaint<T>> implements BaseNonRelationalValueDomain<T> {

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
	 * Yields the domain element that represents tainted values.
	 * 
	 * @return the tainted domain element
	 */
	protected abstract T tainted();

	/**
	 * Yields the domain element that represents clean values.
	 * 
	 * @return the clean domain element
	 */
	protected abstract T clean();

	/**
	 * Yields {@code true} if this instance represents information that is
	 * definitely tainted across all execution paths.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public abstract boolean isAlwaysTainted();

	/**
	 * Yields {@code true} if this instance represents information that is
	 * definitely tainted in at least one execution path.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public abstract boolean isPossiblyTainted();

	/**
	 * Yields {@code true} if this instance represents information that is
	 * definitely clean across all execution paths. By default, this method
	 * returns {@code true} if this is not the bottom instance and
	 * {@link #isPossiblyTainted()} returns {@code false}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isAlwaysClean() {
		return !isPossiblyTainted() && !isBottom();
	}

	/**
	 * Yields {@code true} if this instance represents information that is
	 * definitely clean in at least one execution path. By default, this method
	 * returns {@code true} if {@link #isAlwaysTainted()} returns {@code false}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isPossiblyClean() {
		return !isAlwaysTainted();
	}

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
	protected T defaultApprox(
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
	public T fixedVariable(
			Identifier id,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		T def = defaultApprox(id, pp, oracle);
		if (!def.isBottom())
			return def;
		return BaseNonRelationalValueDomain.super.fixedVariable(id, pp, oracle);
	}

	@Override
	public T evalIdentifier(
			Identifier id,
			ValueEnvironment<T> environment,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		T def = defaultApprox(id, pp, oracle);
		if (!def.isBottom())
			return def;
		return BaseNonRelationalValueDomain.super.evalIdentifier(id, environment, pp, oracle);
	}

	@Override
	public T evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return top();
	}

	@Override
	public T evalNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return clean();
	}

	@Override
	public T evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return clean();
	}

	@Override
	public T evalUnaryExpression(
			UnaryOperator operator,
			T arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return arg;
	}

	@Override
	public T evalBinaryExpression(
			BinaryOperator operator,
			T left,
			T right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return left.lub(right);
	}

	@Override
	public T evalTernaryExpression(
			TernaryOperator operator,
			T left,
			T middle,
			T right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return left.lub(middle).lub(right);
	}

	@Override
	public T evalTypeCast(
			BinaryExpression cast,
			T left,
			T right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return left;
	}

	@Override
	public T evalTypeConv(
			BinaryExpression conv,
			T left,
			T right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return left;
	}

	@Override
	public boolean canProcess(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		try {
			Type dyn = oracle.getDynamicTypeOf(expression, pp, oracle);
			return !dyn.isPointerType() && !dyn.isInMemoryType();
		} catch (SemanticException e) {
			return false;
		}
	}

	@Override
	public Satisfiability satisfies(
			ValueExpression expression,
			ValueEnvironment<T> environment,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// quick answer: we cannot do anything
		return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<T> assume(
			ValueEnvironment<T> environment,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		// quick answer: we cannot do anything
		return environment;
	}
}
