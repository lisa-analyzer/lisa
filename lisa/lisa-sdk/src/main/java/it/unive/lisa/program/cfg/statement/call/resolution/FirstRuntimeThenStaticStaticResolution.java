package it.unive.lisa.program.cfg.statement.call.resolution;

import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * A strategy where the first parameter is tested using
 * {@link RuntimeTypesResolution}, while the rest is tested using
 * {@link StaticTypesResolution}, achieving Java-like resolution.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class FirstRuntimeThenStaticStaticResolution extends FixedOrderResolution {

	/**
	 * The singleton instance of this class.
	 */
	public static final FirstRuntimeThenStaticStaticResolution INSTANCE = new FirstRuntimeThenStaticStaticResolution();

	private FirstRuntimeThenStaticStaticResolution() {
	}

	@Override
	protected boolean matches(int pos, Parameter formal, Expression actual) {
		return pos == 0 ? RuntimeTypesResolution.INSTANCE.matches(pos, formal, actual)
				: StaticTypesResolution.INSTANCE.matches(pos, formal, actual);
	}
}
