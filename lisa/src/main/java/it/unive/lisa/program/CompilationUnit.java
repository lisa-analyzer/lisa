package it.unive.lisa.program;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;

public class CompilationUnit extends Unit {

	private final Collection<CompilationUnit> superUnits;

	private final Collection<CompilationUnit> instances;

	private final Map<String, Global> instanceGlobals;

	private final Map<String, CFG> instanceCfgs;

	private boolean hierarchyComputed;

	public CompilationUnit(String sourceFile, int line, int col, String name) {
		super(sourceFile, line, col, name);
		superUnits = Collections.newSetFromMap(new ConcurrentHashMap<>());
		instances = Collections.newSetFromMap(new ConcurrentHashMap<>());
		instanceGlobals = new ConcurrentHashMap<>();
		instanceCfgs = new ConcurrentHashMap<>();

		hierarchyComputed = false;
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

	public boolean addSuperUnit(CompilationUnit unit) {
		return superUnits.add(unit);
	}

	public boolean addSuperUnits(Collection<? extends CompilationUnit> units) {
		return this.superUnits.addAll(units);
	}

	public boolean addInstanceGlobal(Global global) {
		return instanceGlobals.putIfAbsent(global.getName(), global) == null;
	}

	public boolean addInstanceGlobals(Collection<? extends Global> globals) {
		AtomicBoolean bool = new AtomicBoolean(true);
		globals.forEach(g -> bool.set(bool.get() && addInstanceGlobal(g)));
		return bool.get();
	}

	public boolean addInstanceCFG(CFG cfg) {
		return instanceCfgs.putIfAbsent(cfg.getDescriptor().getSignature(), cfg) == null;
	}

	public boolean addInstanceCFGs(Collection<? extends CFG> cfgs) {
		AtomicBoolean bool = new AtomicBoolean(true);
		cfgs.forEach(c -> bool.set(bool.get() && addInstanceCFG(c)));
		return bool.get();
	}

	public CFG getMatchingInstanceCFG(CFGDescriptor signature, boolean traverseHierarchy) {
		for (CFG cfg : instanceCfgs.values())
			if (cfg.getDescriptor().matchesSignature(signature))
				return cfg;

		if (!traverseHierarchy)
			return null;

		Optional<CFG> sup = superUnits.stream().map(u -> u.getMatchingInstanceCFG(signature, true))
				.filter(c -> c != null).findFirst();
		if (sup.isEmpty())
			return null;

		return sup.get();
	}

	public boolean isInstanceOf(CompilationUnit unit) {
		return this == unit || superUnits.stream().anyMatch(u -> u.isInstanceOf(unit));
	}

	private void addInstance(CompilationUnit unit) {
		instances.add(unit);
		superUnits.forEach(s -> s.addInstance(unit));
	}

	public void computeHierarchy() {
		if (hierarchyComputed)
			return;

		superUnits.forEach(s -> s.computeHierarchy());
		addInstance(this);

		CFG over;
		for (CFG cfg : instanceCfgs.values())
			for (CompilationUnit s : superUnits)
				if ((over = s.getMatchingInstanceCFG(cfg.getDescriptor(), true)) != null
						&& over.getDescriptor().isOverridable()) {
					cfg.getDescriptor().overrides().addAll(over.getDescriptor().overrides());
					cfg.getDescriptor().overrides().add(over);
					cfg.getDescriptor().overrides().forEach(c -> c.getDescriptor().overriddenBy().add(cfg));
				}

		hierarchyComputed = true;
	}

	public CFG getInstanceCFG(String signature) {
		return instanceCfgs.get(signature);
	}

	public Collection<CFG> getInstanceCFGsByName(String name, boolean traverseHierarchy) {
		Collection<CFG> result = Collections.emptySet();
		for (CFG cfg : instanceCfgs.values())
			if (cfg.getDescriptor().getName().equals(name))
				result.add(cfg);

		if (!traverseHierarchy)
			return result;

		for (CompilationUnit cu : superUnits)
			for (CFG sup : cu.getInstanceCFGsByName(name, true))
				if (result.stream().anyMatch(cfg -> sup.getDescriptor().overriddenBy().contains(cfg)))
					continue;
				else
					result.add(sup);

		return result;
	}

	public Global getInstanceGlobal(String name) {
		return instanceGlobals.get(name);
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
}
