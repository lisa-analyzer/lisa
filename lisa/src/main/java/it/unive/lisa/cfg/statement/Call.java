package it.unive.lisa.cfg.statement;

import java.util.Arrays;
import java.util.Objects;

import it.unive.lisa.cfg.CFG;

/**
 * A call to another procedure. This concrete instance of this class determines
 * whether this class represent a true call to another CFG (either in or out of
 * the analysis), or if it represents the invocation of one of the native
 * constructs of the language.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Call extends Expression {

	/**
	 * The parameters of this call
	 */
	private final Expression[] parameters;

	/**
	 * Builds a call happening at the given source location.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param sourceFile the source file where this expression happens. If unknown,
	 *                   use {@code null}
	 * @param line       the line number where this expression happens in the source
	 *                   file. If unknown, use {@code -1}
	 * @param col        the column where this expression happens in the source
	 *                   file. If unknown, use {@code -1}
	 * @param parameters the parameters of this call
	 */
	protected Call(CFG cfg, String sourceFile, int line, int col, Expression... parameters) {
		super(cfg, sourceFile, line, col);
		Objects.requireNonNull(parameters, "The array of parameters of a call cannot be null");
		for (int i = 0; i < parameters.length; i++)
			Objects.requireNonNull(parameters[i], "The " + i + "-th parameter of a call cannot be null");
		this.parameters = parameters;
	}

	/**
	 * Yields the parameters of this call.
	 * 
	 * @return the parameters of this call
	 */
	public final Expression[] getParameters() {
		return parameters;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(parameters);
		return result;
	}

	@Override
	public boolean isEqualTo(Statement st) {
		if (this == st)
			return true;
		if (getClass() != st.getClass())
			return false;
		Call other = (Call) st;
		if (!areEquals(parameters, other.parameters))
			return false;
		return true;
	}

	private static boolean areEquals(Expression[] params, Expression[] otherParams) {
		if (params == otherParams)
			return true;
		
		if (params == null || otherParams == null)
			return false;

		int length = params.length;
		if (otherParams.length != length)
			return false;

		for (int i = 0; i < length; i++)
			if (!isEqualTo(params[i], otherParams[i]))
				return false;

		return true;
	}

	private static boolean isEqualTo(Expression a, Expression b) {
		return (a == b) || (a != null && a.isEqualTo(b));
	}
}
