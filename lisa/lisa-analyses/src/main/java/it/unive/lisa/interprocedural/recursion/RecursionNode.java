package it.unive.lisa.interprocedural.recursion;

import java.util.Collections;
import java.util.Set;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.call.CFGCall;

public class RecursionNode {

	private final Set<CFGCall> calls;

	private final CFG target;

	public RecursionNode(CFGCall call, CFG target) {
		this(Collections.singleton(call), target);
	}

	public RecursionNode(Set<CFGCall> calls, CFG target) {
		this.calls = calls;
		this.target = target;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((calls == null) ? 0 : calls.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RecursionNode other = (RecursionNode) obj;
		if (calls == null) {
			if (other.calls != null)
				return false;
		} else if (!calls.equals(other.calls))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}

	public Set<CFGCall> getCalls() {
		return calls;
	}

	public CFG getTarget() {
		return target;
	}

	public boolean equalsUpToCalls(RecursionNode other) {
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}
}
