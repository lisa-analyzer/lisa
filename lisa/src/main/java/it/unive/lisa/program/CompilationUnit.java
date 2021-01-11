package it.unive.lisa.program;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.NativeCFG;

public class CompilationUnit extends Unit {

	private final Collection<CompilationUnit> superUnits;

	private final Collection<CompilationUnit> instances;

	private final Map<String, Global> instanceGlobals;

	private final Map<String, CFG> instanceCfgs;

	private final Map<String, NativeCFG> instanceConstructs;

	private final boolean sealed;

	private boolean hierarchyComputed;

	public CompilationUnit(String sourceFile, int line, int col, String name, boolean sealed) {
		super(sourceFile, line, col, name);
		this.sealed = sealed;
		superUnits = Collections.newSetFromMap(new ConcurrentHashMap<>());
		instances = Collections.newSetFromMap(new ConcurrentHashMap<>());
		instanceGlobals = new ConcurrentHashMap<>();
		instanceCfgs = new ConcurrentHashMap<>();
		instanceConstructs = new ConcurrentHashMap<>();
		hierarchyComputed = false;
	}

	public boolean isSealed() {
		return sealed;
	}

	public Collection<CompilationUnit> getSuperUnits() {
		return superUnits;
	}

	public Collection<CompilationUnit> getInstances() {
		return instances;
	}

	public Collection<Global> getInstanceGlobals() {
		return instanceGlobals.values();
	}

	public Collection<CFG> getInstanceCFGs() {
		return instanceCfgs.values();
	}

	public Collection<NativeCFG> getInstanceConstructs() {
		return instanceConstructs.values();
	}

	public boolean addSuperUnit(CompilationUnit unit) {
		return superUnits.add(unit);
	}

	public boolean addInstanceGlobal(Global global) {
		return instanceGlobals.putIfAbsent(global.getName(), global) == null;
	}

	public boolean addInstanceCFG(CFG cfg) {
		CFG c = instanceCfgs.putIfAbsent(cfg.getDescriptor().getSignature(), cfg);
		if (sealed)
			if (c == null)
				cfg.getDescriptor().setOverridable(false);
			else
				c.getDescriptor().setOverridable(false);
		return c == null;
	}

	public boolean addInstanceConstruct(NativeCFG construct) {
		NativeCFG c = instanceConstructs.putIfAbsent(construct.getDescriptor().getSignature(), construct);
		if (sealed)
			if (c == null)
				construct.getDescriptor().setOverridable(false);
			else
				c.getDescriptor().setOverridable(false);
		return c == null;
	}

	public CFG getInstanceCFG(String signature) {
		return instanceCfgs.get(signature);
	}

	public NativeCFG getInstanceConstruct(String signature) {
		return instanceConstructs.get(signature);
	}

	public Global getInstanceGlobal(String name) {
		return instanceGlobals.get(name);
	}

	public Collection<NativeCFG> getInstanceConstructsByName(String name) {
		return instanceConstructs.values().stream().filter(c -> c.getDescriptor().getName().equals(name))
				.collect(Collectors.toList());
	}

	public Collection<CodeMember> getMatchingInstanceCodeMember(CFGDescriptor signature, boolean traverseHierarchy) {
		return searchCodeMembers(cm -> cm.getDescriptor().matchesSignature(signature), traverseHierarchy);
	}

	public Collection<CodeMember> getInstanceCodeMembersByName(String name, boolean traverseHierarchy) {
		return searchCodeMembers(cm -> cm.getDescriptor().getName().equals(name), traverseHierarchy);
	}

	private Collection<CodeMember> searchCodeMembers(Function<CodeMember, Boolean> filter, boolean traverseHierarchy) {
		Collection<CodeMember> result = new HashSet<>();
		for (CFG cfg : instanceCfgs.values())
			if (filter.apply(cfg))
				result.add(cfg);

		for (NativeCFG construct : instanceConstructs.values())
			if (filter.apply(construct))
				result.add(construct);

		if (!traverseHierarchy)
			return result;

		for (CompilationUnit cu : superUnits)
			for (CodeMember sup : cu.searchCodeMembers(filter, true))
				if (result.stream().anyMatch(cfg -> sup.getDescriptor().overriddenBy().contains(cfg)))
					continue;
				else
					result.add(sup);

		return result;
	}

	@Override
	public Collection<CFG> getAllCFGs() {
		Collection<CFG> all = super.getAllCFGs();
		instanceCfgs.values().forEach(all::add);
		return all;
	}

	@Override
	public Collection<Global> getAllGlobals() {
		Collection<Global> all = super.getAllGlobals();
		instanceGlobals.values().forEach(all::add);
		return all;
	}

	@Override
	public Collection<NativeCFG> getAllConstructs() {
		Collection<NativeCFG> all = super.getAllConstructs();
		instanceConstructs.values().forEach(all::add);
		return all;
	}

	public Collection<CodeMember> getInstanceCodeMembers() {
		HashSet<CodeMember> all = new HashSet<>(getInstanceCFGs());
		all.addAll(getInstanceConstructs());
		return all;
	}

	public boolean isInstanceOf(CompilationUnit unit) {
		return this == unit || superUnits.stream().anyMatch(u -> u.isInstanceOf(unit));
	}

	private void addInstance(CompilationUnit unit) {
		instances.add(unit);
		superUnits.forEach(s -> s.addInstance(unit));
	}

	public void validateAndFinalize() throws ProgramValidationException {
		if (hierarchyComputed)
			return;

		for (CompilationUnit sup : superUnits)
			if (sup.sealed)
				throw new ProgramValidationException(this + " cannot inherit from the sealed unit " + sup);
			else
				sup.validateAndFinalize();
		addInstance(this);

		for (CodeMember cfg : getInstanceCodeMembers()) {
			Collection<CodeMember> matching = getMatchingInstanceCodeMember(cfg.getDescriptor(), false);
			if (matching.size() != 1 || matching.iterator().next() != cfg)
				throw new ProgramValidationException(
						cfg.getDescriptor().getSignature() + " is duplicated within unit " + this);

			for (CompilationUnit s : superUnits)
				for (CodeMember over : s.getMatchingInstanceCodeMember(cfg.getDescriptor(), true))
					if (over.getDescriptor().isOverridable()) {
						cfg.getDescriptor().overrides().addAll(over.getDescriptor().overrides());
						cfg.getDescriptor().overrides().add(over);
						cfg.getDescriptor().overrides().forEach(c -> c.getDescriptor().overriddenBy().add(cfg));
					} else
						throw new ProgramValidationException(
								this + " overrides the non-overridable cfg " + over.getDescriptor().getSignature());
		}

		hierarchyComputed = true;
	}
}
