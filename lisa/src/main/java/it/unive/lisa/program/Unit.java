package it.unive.lisa.program;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import it.unive.lisa.program.cfg.CFG;

public abstract class Unit extends CodeElement {
	
	private final String name;
	
	private final Collection<Global> globals;
	
	private final Collection<CFG> cfgs;

	protected Unit(String sourceFile, int line, int col, String name) {
		super(sourceFile, line, col);
		this.name = name;
		this.globals = Collections.newSetFromMap(new ConcurrentHashMap<>());
		this.cfgs = Collections.newSetFromMap(new ConcurrentHashMap<>());
	}

	public String getName() {
		return name;
	}
	
	public Collection<Global> getGlobals() {
		return globals;
	}

	public Collection<CFG> getCfgs() {
		return cfgs;
	}

	public boolean addGlobal(Global global) {
		return globals.add(global);
	}

	public boolean addGlobals(Collection<? extends Global> globals) {
		return this.globals.addAll(globals);
	}

	public boolean addCFG(CFG cfg) {
		return cfgs.add(cfg);
	}

	public boolean addCFGs(Collection<? extends CFG> cfgs) {
		return this.cfgs.addAll(cfgs);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Unit other = (Unit) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	@Override
	public final String toString() {
		return name;
	}
}
