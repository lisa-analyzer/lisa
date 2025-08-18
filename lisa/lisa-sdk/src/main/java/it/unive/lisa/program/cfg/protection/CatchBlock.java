package it.unive.lisa.program.cfg.protection;

import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.type.Type;
import java.util.Arrays;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * A catch block that is part of a {@link ProtectionBlock}. It can handle
 * specific exceptions and can optionally store the caught exception in a
 * variable.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CatchBlock {

	private final Type[] exceptions;

	private final VariableRef identifier;

	private final ProtectedBlock body;

	/**
	 * Builds a catch block.
	 * 
	 * @param identifier the identifier where the caught exception is stored, if
	 *                       any; if this catch block does not have an
	 *                       identifier, this should be {@code null}
	 * @param body       the body of the catch block
	 * @param exceptions the exceptions that this catch block can handle
	 */
	public CatchBlock(
			VariableRef identifier,
			ProtectedBlock body,
			Type... exceptions) {
		Objects.requireNonNull(body, "The body of a catch block cannot be null");
		Objects.requireNonNull(exceptions, "The exceptions of a catch block cannot be null");
		if (exceptions.length == 0)
			throw new IllegalArgumentException("A catch block must handle at least one exception");
		this.exceptions = exceptions;
		this.identifier = identifier;
		this.body = body;
	}

	/**
	 * Yields the exceptions that this catch block can handle.
	 * 
	 * @return the exceptions that this catch block can handle
	 */
	public Type[] getExceptions() {
		return exceptions;
	}

	/**
	 * Yields the identifier where the caught exception is stored, if any. If
	 * this catch block does not have an identifier, this method returns
	 * {@code null}.
	 * 
	 * @return the identifier where the caught exception is stored, or
	 *             {@code null} if there is none
	 */
	public VariableRef getIdentifier() {
		return identifier;
	}

	/**
	 * Yields the body of this catch block, which is a {@link ProtectedBlock}.
	 * 
	 * @return the body of this catch block
	 */
	public ProtectedBlock getBody() {
		return body;
	}

	/**
	 * Simplifies this block, removing all {@link NoOp}s from its body.
	 */
	public void simplify() {
		body.getBody().removeIf(NoOp.class::isInstance);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((exceptions == null) ? 0 : Arrays.hashCode(exceptions));
		result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CatchBlock other = (CatchBlock) obj;
		if (exceptions == null) {
			if (other.exceptions != null)
				return false;
		} else if (!Arrays.equals(exceptions, other.exceptions))
			return false;
		if (identifier == null) {
			if (other.identifier != null)
				return false;
		} else if (!identifier.equals(other.identifier))
			return false;
		if (body == null) {
			if (other.body != null)
				return false;
		} else if (!body.equals(other.body))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "catch (" + StringUtils.join(exceptions, ", ") + ") " + identifier;
	}
}
