package it.unive.lisa.program.language.resolution;

import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.NamedParameterExpression;
import it.unive.lisa.type.Type;
import java.util.Set;

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
	@SuppressWarnings("unchecked")
	public boolean matches(Call call, Parameter[] formals, Expression[] actuals, Set<Type>[] types) {
		Expression[] slots = new Expression[formals.length];
		Set<Type>[] slotTypes = new Set[formals.length];

		Expression[] defaults = new Expression[formals.length];
		Set<Type>[] defaultTypes = new Set[formals.length];
		for (int pos = 0; pos < slots.length; pos++) {
			Expression def = formals[pos].getDefaultValue();
			if (def != null) {
				defaults[pos] = def;
				defaultTypes[pos] = def.getStaticType().allInstances(call.getProgram().getTypes());
			}
		}

		Boolean logic = PythonLikeMatchingStrategy.pythonLogic(
				formals,
				actuals,
				actuals,
				types,
				defaults,
				defaultTypes,
				slots,
				slotTypes,
				false);
		if (logic != null)
			return logic;

		return delegate.matches(call, formals, slots, slotTypes);
	}

	/**
	 * Python logic for preparing an argument list for a method call. If the
	 * preparation fails, that is, if the signatures are incompatible,
	 * {@code failure} is returned. Otherwise, {@code null} is returned.
	 * {@code slots} is filled with the positional parameters from {@code given}
	 * first (that is, ones corresponding to an actual in {@code actuals} that
	 * is not an instance of {@link NamedParameterExpression}), then named
	 * parameters from {@code given}, and lastly with default values from
	 * {@code defaults}.
	 * 
	 * @param <T>          the type of elements in {@code slots}
	 * @param <F>          the type of the element returned if the preparation
	 *                         fails
	 * @param formals      the formal parameters
	 * @param actuals      the actual parameters
	 * @param given        the value to use for each parameter, positional or
	 *                         named
	 * @param givenTypes   the types of the value to use for each parameter,
	 *                         positional or named
	 * @param defaults     the default values for each parameter if no explicit
	 *                         value is provided
	 * @param defaultTypes the types of the default values for each parameter if
	 *                         no explicit value is provided
	 * @param slots        the slots that represent final values to use as
	 *                         parameters
	 * @param slotTypes    the types of the slots that represent final values to
	 *                         use as parameters
	 * @param failure      what to return in case of failure
	 * 
	 * @return {@code failure} if the preparation fails, {@code null} otherwise
	 * 
	 * @see <a href=
	 *          "https://docs.python.org/3/reference/expressions.html#calls">Python
	 *          Language Reference: calls</a>
	 */
	public static <T, F> F pythonLogic(
			Parameter[] formals,
			Expression[] actuals,
			T[] given,
			Set<Type>[] givenTypes,
			T[] defaults,
			Set<Type>[] defaultTypes,
			T[] slots,
			Set<Type>[] slotTypes,
			F failure) {
		if (formals.length < actuals.length)
			// too many arguments!
			return failure;

		int pos = 0;

		// first phase: positional arguments
		// we stop at the first named parameter
		for (; pos < actuals.length; pos++)
			if (actuals[pos] instanceof NamedParameterExpression)
				break;
			else {
				slots[pos] = given[pos];
				slotTypes[pos] = givenTypes[pos];
			}

		// second phase: keyword arguments
		for (; pos < actuals.length; pos++) {
			String name = ((NamedParameterExpression) actuals[pos]).getParameterName();
			for (int i = pos; i < formals.length; i++)
				if (formals[i].getName().equals(name)) {
					if (slots[i] != null)
						// already filled -> TypeError
						return failure;
					else {
						slots[i] = given[pos];
						slotTypes[i] = givenTypes[pos];
					}
					break;
				}
		}

		// third phase: default values
		for (pos = 0; pos < slots.length; pos++)
			if (slots[pos] == null) {
				if (defaults[pos] == null)
					// unfilled and no default value -> TypeError
					return failure;
				else {
					slots[pos] = defaults[pos];
					slotTypes[pos] = defaultTypes[pos];
				}
			}

		return null;
	}
}
