package it.unive.lisa.program.cfg.statement.call.resolution;

import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * A strategy where the static types of the parameters of the call are evaluated
 * against the signature of a cfg: for each parameter, if the static type of the
 * actual parameter can be assigned to the type of the formal parameter, than
 * {@link #matches(Parameter[], Expression[])} return {@code true}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StaticTypesResolution extends FixedOrderResolution {

	/**
	 * The singleton instance of this class.
	 */
	public static final StaticTypesResolution INSTANCE = new StaticTypesResolution();

	private StaticTypesResolution() {
	}

	@Override
	protected boolean matches(int pos, Parameter formal, Expression actual) {
		return actual.getStaticType().canBeAssignedTo(formal.getStaticType());
	}
}
