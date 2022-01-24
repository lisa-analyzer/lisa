package it.unive.lisa.program.cfg.statement.call.resolution;

import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;

/**
 * A strategy where the first parameter is tested using
 * {@link RuntimeTypesResolution} if it represents the receiver of an instance
 * call, otherwise it is tested with {@link StaticTypesResolution}. Other
 * parameters are always tested using {@link StaticTypesResolution}, achieving
 * Java-like resolution.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class RuntimeReceiverThenStaticStaticResolution extends FixedOrderResolution {

	/**
	 * The singleton instance of this class.
	 */
	public static final RuntimeReceiverThenStaticStaticResolution INSTANCE = new RuntimeReceiverThenStaticStaticResolution();

	private RuntimeReceiverThenStaticStaticResolution() {
	}

	@Override
	protected boolean matches(Call call, int pos, Parameter formal, Expression actual) {
		if (call.isInstanceCall() && pos == 0)
			return RuntimeTypesResolution.INSTANCE.matches(call, pos, formal, actual);
		return StaticTypesResolution.INSTANCE.matches(call, pos, formal, actual);
	}
}
