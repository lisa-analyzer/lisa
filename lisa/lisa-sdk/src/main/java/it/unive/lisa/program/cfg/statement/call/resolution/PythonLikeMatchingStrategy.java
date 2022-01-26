package it.unive.lisa.program.cfg.statement.call.resolution;

import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.NamedParameterExpression;

/**
 * A Python-like matching strategy. Specifically, actual parameters are
 * rearranged following the specification:<br>
 * <i>If keyword arguments are present, they are first converted to positional
 * arguments, as follows. First, a list of unfilled slots is created for the
 * formal parameters. If there are N positional arguments, they are placed in
 * the first N slots. Next, for each keyword argument, the identifier is used to
 * determine the corresponding slot (if the identifier is the same as the first
 * formal parameter name, the first slot is used, and so on). If the slot is
 * already filled, a TypeError exception is raised. Otherwise, the value of the
 * argument is placed in the slot, filling it (even if the expression is None,
 * it fills the slot). When all arguments have been processed, the slots that
 * are still unfilled are filled with the corresponding default value from the
 * function definition. [...] If there are any unfilled slots for which no
 * default value is specified, a TypeError exception is raised. Otherwise, the
 * list of filled slots is used as the argument list for the call. [...] If
 * there are more positional arguments than there are formal parameter slots, a
 * TypeError exception is raised [...]. If any keyword argument does not
 * correspond to a formal parameter name, a TypeError exception is raised
 * [...].</i><br>
 * The only difference w.r.t. this specification is that whenever a TypeError
 * exception should be raised, the matching interrupts and {@code false} is
 * returned.<br>
 * <br>
 * Keyword parameters are identified by actuals (i.e. {@link Expression}s) that
 * are instance of {@link NamedParameterExpression}.<br>
 * <br>
 * After the reordering is completed, the type-based matching of each parameter
 * happens by delegation to a specified {@link FixedOrderMatchingStrategy}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @see <a href=
 *          "https://docs.python.org/3/reference/expressions.html#calls">Python
 *          Language Reference: calls</a>
 */
public class PythonLikeMatchingStrategy implements ParameterMatchingStrategy {

	private final FixedOrderMatchingStrategy delegate;

	/**
	 * Builds the strategy.
	 * 
	 * @param delegate the strategy to delegate the match after the actual
	 *                     parameters have been shuffled
	 */
	public PythonLikeMatchingStrategy(FixedOrderMatchingStrategy delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean matches(Call call, Parameter[] formals, Expression[] actuals) {
		Expression[] slots = new Expression[formals.length];
		int pos = 0;

		// first phase: positional arguments
		// we stop at the first named parameter
		for (; pos < actuals.length; pos++)
			if (actuals[pos] instanceof NamedParameterExpression)
				break;
			else
				slots[pos] = actuals[pos];

		// second phase: keyword arguments
		for (; pos < actuals.length; pos++) {
			String name = ((NamedParameterExpression) actuals[pos]).getParameterName();
			for (int i = pos; i < formals.length; i++)
				if (formals[i].getName().equals(name)) {
					if (slots[i] != null)
						// already filled -> TypeError
						return false;
					else
						slots[i] = actuals[pos];
					break;
				}
		}

		// third phase: default values
		for (; pos < actuals.length; pos++)
			if (slots[pos] == null) {
				Expression def = formals[pos].getDefaultValue();
				if (def == null)
					// unfilled and no default value -> TypeError
					return false;
				else
					slots[pos] = def;
			}

		return delegate.matches(call, formals, slots);
	}
}
