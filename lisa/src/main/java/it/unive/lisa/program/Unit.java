package it.unive.lisa.program;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import it.unive.lisa.program.cfg.CFG;

public abstract class Unit extends CodeElement {
	
	private final String name;
	
	private final Map<String, Global> globals;
	
	private final Map<String, CFG> cfgs;

	protected Unit(String sourceFile, int line, int col, String name) {
		super(sourceFile, line, col);
		this.name = name;
		this.globals = new ConcurrentHashMap<>();
		this.cfgs = new ConcurrentHashMap<>();
	}

	public String getName() {
		return name;
	}
	
	public Collection<Global> getGlobals() {
		return globals.values();
	}

	public Collection<CFG> getCFGs() {
		return cfgs.values();
	}
	
	public CFG getCFG(String signature) {
		return cfgs.get(signature);
	}
	
	public Collection<CFG> getCFGsByName(String name) {
		return cfgs.values().stream().filter(c -> c.getDescriptor().getName().equals(name)).collect(Collectors.toList());
	}
	
	public Global getGlobal(String name) {
		return globals.get(name);
	}
	
	public Collection<CFG> getAllCFGs() {
		return new HashSet<>(getCFGs());
	}
	
	public Collection<Global> getAllGlobals() {
		return new HashSet<>(getGlobals());
	}

	public boolean addGlobal(Global global) {
		return globals.putIfAbsent(global.getName(), global) == null;
	}

	public boolean addGlobals(Collection<? extends Global> globals) {
		AtomicBoolean bool = new AtomicBoolean(true);
		globals.forEach(g ->  bool.set(bool.get() && addGlobal(g)));
		return bool.get();
	}

	public boolean addCFG(CFG cfg) {
		return cfgs.putIfAbsent(cfg.getDescriptor().getSignature(), cfg) == null;
	}

	public boolean addCFGs(Collection<? extends CFG> cfgs) {
		AtomicBoolean bool = new AtomicBoolean(true);
		cfgs.forEach(c ->  bool.set(bool.get() && addCFG(c)));
		return bool.get();
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
