package it.unive.lisa.program.language.resolution;

import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * A resolution strategy that does not permit by-name (e.g. Python style)
 * parameter assignment that can shuffle parameters order.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class FixedOrderMatchingStrategy implements ParameterMatchingStrategy {

	@Override
	public final boolean matches(Call call, Parameter[] formals, Expression[] actuals, Set<Type>[] types) {
		if (formals.length != actuals.length)
			return false;

		for (int i = 0; i < formals.length; i++)
			if (!matches(call, i, formals[i], actuals[i], types[i]))
				return false;

		return true;
	}

	/**
	 * Yields {@code true} if and only if the signature of the {@code pos}-th
	 * parameter of a cfg is matched by the given actual parameter, according to
	 * this strategy.
	 * 
	 * @param call   the call where the parameters are being matched
	 * @param pos    the position of the parameter being evaluated
	 * @param formal the parameter definition of the cfg
	 * @param actual the expression that is used as parameter
	 * @param types  the runtime types of the actual parameter
	 * 
	 * @return {@code true} if and only if that condition holds
	 */
	public abstract boolean matches(Call call, int pos, Parameter formal, Expression actual, Set<Type> types);
}
