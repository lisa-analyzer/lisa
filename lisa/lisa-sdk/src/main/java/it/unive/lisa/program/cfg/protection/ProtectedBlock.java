package it.unive.lisa.program.cfg.protection;

import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Collection;

public class ProtectedBlock {

	private Statement start;

	private Statement end;

	private final Collection<Statement> body;

	public ProtectedBlock(
			Statement start,
			Statement end,
			Collection<Statement> body) {
		this.start = start;
		this.end = end;
		this.body = body;
	}

	public Statement getStart() {
		return start;
	}

	public void setStart(
			Statement start) {
		this.start = start;
	}

	public Statement getEnd() {
		return end;
	}

	public void setEnd(
			Statement end) {
		this.end = end;
	}

	public Collection<Statement> getBody() {
		return body;
	}

	public boolean canBeContinued() {
		return end != null && !end.stopsExecution();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((start == null) ? 0 : start.hashCode());
		result = prime * result + ((end == null) ? 0 : end.hashCode());
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
		ProtectedBlock other = (ProtectedBlock) obj;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		if (end == null) {
			if (other.end != null)
				return false;
		} else if (!end.equals(other.end))
			return false;
		if (body == null) {
			if (other.body != null)
				return false;
		} else if (!body.equals(other.body))
			return false;
		return true;
	}

}
