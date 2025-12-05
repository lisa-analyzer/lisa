package it.unive.lisa.program.language.resolution;

import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
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
	boolean matches(
			Call call,
			Parameter[] formals,
			Expression[] actuals,
			Set<Type>[] types);

	/**
	 * Computes the "distance" between a candidate code member and a perfect
	 * match for a call.
	 * <p>
	 * Distance reflects type compatibility: zero means an exact match, higher
	 * values mean progressively less exact matches, and negative means
	 * incomparable.
	 * </p>
	 * <p>
	 * The default implementation computes the sum of each parameter's distance,
	 * evaluated through {@link TypeSystem#distanceBetweenTypes(Type, Type)}.
	 * </p>
	 *
	 * @param call     the unresolved call
	 * @param types    the runtime types of the call parameters
	 * @param cm       the candidate code member
	 * @param instance whether the call is an instance call
	 *
	 * @return the computed distance, or {@code -1} if incomparable
	 */
	default int distanceFromPerfectTarget(
			UnresolvedCall call,
			Set<Type>[] types,
			CodeMember cm,
			boolean instance) {
		int distance = 0;
		int startIdx = instance ? 1 : 0;
		Expression[] params = call.getParameters();
		CodeMemberDescriptor descriptor = cm.getDescriptor();
		for (int i = startIdx; i < params.length; i++) {
			Expression parameter = params[i];
			Type paramType = parameter.getStaticType();
			Type formalType = descriptor.getFormals()[i].getStaticType();
			if (formalType instanceof Untyped)
				return 0;
			if (paramType instanceof Untyped) {
				boolean allIncomparable = true;
				for (Type runtimeType : types[i]) {
					int dist = call.getProgram().getTypes().distanceBetweenTypes(formalType, runtimeType);
					if (dist >= 0) {
						allIncomparable = false;
						distance += dist;
					}
				}
				if (allIncomparable)
					return -1;
				continue;
			}
			int dist = call.getProgram().getTypes().distanceBetweenTypes(formalType, paramType);
			if (dist < 0)
				return -1;
			distance += dist;
		}
		return distance;
	}

}
