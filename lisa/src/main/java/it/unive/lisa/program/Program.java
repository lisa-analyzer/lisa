package it.unive.lisa.program;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import it.unive.lisa.program.cfg.CFG;

public class Program extends Unit {
	
	private final Collection<CompilationUnit> units;
	
	public Program() {
		super(null, -1, -1, "~LiSAProgram");
		units = Collections.newSetFromMap(new ConcurrentHashMap<>());
	}

	public boolean addCompilationUnit(CompilationUnit unit) {
		return units.add(unit);
	}

	public boolean addCompilationUnits(Collection<? extends CompilationUnit> units) {
		return this.units.addAll(units);
	}
	
	public Collection<CFG> getAllCFGs() {
		Collection<CFG> all = new LinkedList<>(getCfgs());
		
		for (CompilationUnit unit : units) {
			all.addAll(unit.getCfgs());
			all.addAll(unit.getInstanceCfgs());
		}
		
		return all;
	}
}
