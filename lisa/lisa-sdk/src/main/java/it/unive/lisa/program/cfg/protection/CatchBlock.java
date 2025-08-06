package it.unive.lisa.program.cfg.protection;

import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.type.Type;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

public class CatchBlock {

	private final Type[] exceptions;

	private final VariableRef identifier;

	private final ProtectedBlock body;

	public CatchBlock(
			VariableRef identifier,
			ProtectedBlock body,
			Type... exceptions) {
		this.exceptions = exceptions;
		this.identifier = identifier;
		this.body = body;
	}

	public Type[] getExceptions() {
		return exceptions;
	}

	public VariableRef getIdentifier() {
		return identifier;
	}

	public ProtectedBlock getBody() {
		return body;
	}

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
