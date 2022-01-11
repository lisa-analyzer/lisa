package it.unive.lisa.program.cfg.statement.call.resolution;

import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * A strategy where the runtime types of the parameters of the call are
 * evaluated against the signature of a cfg: for each parameter, if at least one
 * of the runtime types of the actual parameter can be assigned to the type of
 * the formal parameter, than {@link #matches(Parameter[], Expression[])} return
 * {@code true}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class RuntimeTypesResolution extends FixedOrderResolution {

	/**
	 * The singleton instance of this class.
	 */
	public static final RuntimeTypesResolution INSTANCE = new RuntimeTypesResolution();

	private RuntimeTypesResolution() {
	}

	@Override
	protected boolean matches(int pos, Parameter formal, Expression actual) {
		return actual.getRuntimeTypes().anyMatch(rt -> rt.canBeAssignedTo(formal.getStaticType()));
	}
}
