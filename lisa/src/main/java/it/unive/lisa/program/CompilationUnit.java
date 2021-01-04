package it.unive.lisa.program;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;

public class CompilationUnit extends Unit {

	private final Collection<CompilationUnit> superUnits;

	private final Collection<CompilationUnit> instances;

	private final Collection<Global> instanceGlobals;

	private final Collection<CFG> instanceCfgs;
	
	private boolean hierarchyComputed;
	
	public CompilationUnit(String sourceFile, int line, int col, String name) {
		super(sourceFile, line, col, name);
		superUnits = Collections.newSetFromMap(new ConcurrentHashMap<>());
		instances = Collections.newSetFromMap(new ConcurrentHashMap<>());
		instanceGlobals = Collections.newSetFromMap(new ConcurrentHashMap<>());
		instanceCfgs = Collections.newSetFromMap(new ConcurrentHashMap<>());
		
		hierarchyComputed = false;
	}
	
	public Collection<CompilationUnit> getSuperUnits() {
		return superUnits;
	}
	
	public Collection<CompilationUnit> getInstances() {
		return instances;
	}
	
	public Collection<Global> getInstanceGlobals() {
		return instanceGlobals;
	}

	public Collection<CFG> getInstanceCfgs() {
		return instanceCfgs;
	}

	public boolean addSuperUnit(CompilationUnit unit) {
		return superUnits.add(unit);
	}

	public boolean addSuperUnits(Collection<? extends CompilationUnit> units) {
		return this.superUnits.addAll(units);
	}
	
	public boolean addInstanceGlobal(Global global) {
		return instanceGlobals.add(global);
	}

	public boolean addInstanceGlobals(Collection<? extends Global> globals) {
		return this.instanceGlobals.addAll(globals);
	}

	public boolean addInstanceCFG(CFG cfg) {
		return instanceCfgs.add(cfg);
	}

	public boolean addInstanceCFGs(Collection<? extends CFG> cfgs) {
		return this.instanceCfgs.addAll(cfgs);
	}
	
	public CFG getMatchingInstanceCFG(CFGDescriptor signature) {
		for (CFG cfg : instanceCfgs)
			if (cfg.getDescriptor().matchesSignature(signature))
				return cfg;
		return null;
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
		for (CFG cfg : instanceCfgs)
			for (CompilationUnit s : superUnits)
				if ((over = s.getMatchingInstanceCFG(cfg.getDescriptor())) != null && over.getDescriptor().isOverridable()) {
					cfg.getDescriptor().overrides().addAll(over.getDescriptor().overrides());
					cfg.getDescriptor().overrides().add(over);
					cfg.getDescriptor().overrides().forEach(c -> c.getDescriptor().overriddenBy().add(cfg));
				}
		
		hierarchyComputed = true;
	}
}
