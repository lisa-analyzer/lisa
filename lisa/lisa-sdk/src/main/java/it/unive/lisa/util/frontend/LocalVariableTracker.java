package it.unive.lisa.util.frontend;

import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.type.Untyped;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class LocalVariableTracker {

	private static class LocalVariable {
		private final CodeLocation location;
		private final Statement scopeStart;
		private final Annotations annotations;

		private LocalVariable(
				CodeLocation location,
				Statement scopeStart,
				Annotations annotations) {
			this.location = location;
			this.scopeStart = scopeStart;
			this.annotations = annotations;
		}
	}

	private final CodeMemberDescriptor descriptor;

	private final List<Map<String, LocalVariable>> visibleIds;

	private Map<String, LocalVariable> latestScope;

	private int varIndex;

	public LocalVariableTracker(
			CFG cfg,
			CodeMemberDescriptor descriptor) {
		this.descriptor = descriptor;
		visibleIds = new LinkedList<>();
		latestScope = new HashMap<>();
		for (VariableTableEntry par : descriptor.getVariables())
			latestScope.put(par.getName(), new LocalVariable(
					par.getLocation(),
					par.createReference(cfg),
					par.getAnnotations()));
		visibleIds.add(latestScope);
		varIndex = descriptor.getVariables().size();
	}

	public void enterScope() {
		latestScope = new HashMap<>();
		visibleIds.add(latestScope);
	}

	public void exitScope(
			Statement closing) {
		if (visibleIds.isEmpty())
			throw new IllegalStateException("Cannot exit scope: no scopes are currently active");

		for (Entry<String, LocalVariable> id : latestScope.entrySet())
			descriptor.addVariable(new VariableTableEntry(
					id.getValue().location,
					varIndex++,
					id.getValue().scopeStart,
					closing,
					id.getKey(),
					Untyped.INSTANCE,
					id.getValue().annotations));

		visibleIds.remove(visibleIds.size() - 1);
		latestScope = visibleIds.get(visibleIds.size() - 1);
	}

	public boolean hasVariable(
			String name) {
		for (Map<String, LocalVariable> scope : visibleIds)
			if (scope.containsKey(name))
				return true;
		return false;
	}

	public void addVariable(
			String name,
			Statement definition,
			Annotations annotations) {
		latestScope.put(name, new LocalVariable(
				definition.getLocation(),
				definition instanceof Expression
						? ((Expression) definition).getRootStatement()
						: definition,
				annotations));
	}
}