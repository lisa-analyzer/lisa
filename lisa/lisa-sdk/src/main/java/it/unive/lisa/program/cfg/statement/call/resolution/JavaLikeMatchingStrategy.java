package it.unive.lisa.program.cfg.statement.call.resolution;

import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;

/**
 * A strategy where the first parameter is tested using
 * {@link RuntimeTypesMatchingStrategy} if it represents the receiver of an instance
 * call, otherwise it is tested with {@link StaticTypesMatchingStrategy}. Other
 * parameters are always tested using {@link StaticTypesMatchingStrategy}, achieving
 * Java-like resolution.
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
	protected boolean matches(Call call, int pos, Parameter formal, Expression actual) {
		if (call.isInstanceCall() && pos == 0)
			return RuntimeTypesMatchingStrategy.INSTANCE.matches(call, pos, formal, actual);
		return StaticTypesMatchingStrategy.INSTANCE.matches(call, pos, formal, actual);
	}
}
