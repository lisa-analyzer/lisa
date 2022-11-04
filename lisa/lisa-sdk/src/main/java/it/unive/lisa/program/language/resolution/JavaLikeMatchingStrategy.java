package it.unive.lisa.program.language.resolution;

import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * A strategy where the first parameter is tested using
 * {@link RuntimeTypesMatchingStrategy} if it represents the receiver of an
 * instance call, otherwise it is tested with
 * {@link StaticTypesMatchingStrategy}. Other parameters are always tested using
 * {@link StaticTypesMatchingStrategy}, achieving Java-like resolution.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class JavaLikeMatchingStrategy extends FixedOrderMatchingStrategy {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaLikeMatchingStrategy INSTANCE = new JavaLikeMatchingStrategy();

	private JavaLikeMatchingStrategy() {
	}

	@Override
	public boolean matches(Call call, int pos, Parameter formal, Expression actual, Set<Type> types) {
		if (call.getCallType() == CallType.INSTANCE && pos == 0)
			return RuntimeTypesMatchingStrategy.INSTANCE.matches(call, pos, formal, actual, types);
		return StaticTypesMatchingStrategy.INSTANCE.matches(call, pos, formal, actual, types);
	}
}
