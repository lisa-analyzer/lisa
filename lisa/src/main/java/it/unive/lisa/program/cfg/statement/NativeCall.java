package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * A native call, modeling the usage of one of the native constructs of the
 * language.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class NativeCall extends Call {

	/**
	 * The target of this call
	 */
	private final String constructName;

	/**
	 * Builds the untyped native call, happening at the given location in the
	 * program. The static type of this call is {@link Untyped}.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param location      the location where the expression is defined within
	 *                          the source file. If unknown, use {@code null}
	 * @param constructName the name of the construct invoked by this native
	 *                          call
	 * @param parameters    the parameters of this call
	 */
	protected NativeCall(CFG cfg, CodeLocation location, String constructName,
			Expression... parameters) {
		this(cfg, location, constructName, Untyped.INSTANCE, parameters);
	}

	/**
	 * Builds the native call, happening at the given location in the program.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param location      the location where this expression is defined within
	 *                          the source file. If unknown, use {@code null}
	 * @param constructName the name of the construct invoked by this native
	 *                          call
	 * @param staticType    the static type of this call
	 * @param parameters    the parameters of this call
	 */
	protected NativeCall(CFG cfg, CodeLocation location, String constructName, Type staticType,
			Expression... parameters) {
		super(cfg, location, staticType, parameters);
		Objects.requireNonNull(constructName, "The name of the native construct of a native call cannot be null");
		this.constructName = constructName;
	}

	/**
	 * Yields the CFG that is targeted by this CFG call.
	 * 
	 * @return the target CFG
	 */
	public final String getConstructName() {
		return constructName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((constructName == null) ? 0 : constructName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		NativeCall other = (NativeCall) obj;
		if (constructName == null) {
			if (other.constructName != null)
				return false;
		} else if (!constructName.equals(other.constructName))
			return false;
		return true;
	}

	@Override
	public final String toString() {
		return constructName + "(" + StringUtils.join(getParameters(), ", ") + ")";
	}
}
