package it.unive.lisa.program;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import it.unive.lisa.program.cfg.CFG;

public class CompilationUnit extends Unit {

	private final Collection<CompilationUnit> superUnits;

	private final Collection<Global> instanceGlobals;

	private final Collection<CFG> instanceCfgs;

	public CompilationUnit(String sourceFile, int line, int col, String name) {
		super(sourceFile, line, col, name);
		superUnits = Collections.newSetFromMap(new ConcurrentHashMap<>());
		instanceGlobals = Collections.newSetFromMap(new ConcurrentHashMap<>());
		instanceCfgs = Collections.newSetFromMap(new ConcurrentHashMap<>());
	}
	
	public Collection<CompilationUnit> getSuperUnits() {
		return superUnits;
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

}
