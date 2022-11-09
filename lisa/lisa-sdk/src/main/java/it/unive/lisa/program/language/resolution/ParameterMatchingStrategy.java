package it.unive.lisa.program.language.resolution;

import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * A strategy for matching call signatures. Depending on the language, targets
 * of calls might be resolved (at compile time or runtime) relying on the static
 * or runtime type of their parameters. Some languages might also have named
 * parameter passing, allowing shuffling of parameters. Each strategy comes with
 * a different {@link #matches(Call, Parameter[], Expression[], Set[])}
 * implementation that can automatically detect if the signature of a cfg is
 * matched by the given expressions representing the parameters for a call to
 * that cfg.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface ParameterMatchingStrategy {

	/**
	 * Yields {@code true} if and only if the parameter list of a cfg is matched
	 * by the given actual parameters, according to this strategy.
	 * 
	 * @param call    the call where the parameters are being matched
	 * @param formals the parameters definition of the cfg
	 * @param actuals the expression that are used as call parameters
	 * @param types   the runtime types of the actual parameters
	 * 
	 * @return {@code true} if and only if that condition holds
	 */
	boolean matches(Call call, Parameter[] formals, Expression[] actuals, Set<Type>[] types);
}
