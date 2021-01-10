package it.unive.lisa.program;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.NativeCFG;

public abstract class Unit extends CodeElement {

	private final String name;

	private final Map<String, Global> globals;

	private final Map<String, CFG> cfgs;

	private final Map<String, NativeCFG> constructs;

	protected Unit(String sourceFile, int line, int col, String name) {
		super(sourceFile, line, col);
		this.name = name;
		this.globals = new ConcurrentHashMap<>();
		this.cfgs = new ConcurrentHashMap<>();
		this.constructs = new ConcurrentHashMap<>();
	}

	public final String getName() {
		return name;
	}

	public final Collection<Global> getGlobals() {
		return globals.values();
	}

	public final Collection<CFG> getCFGs() {
		return cfgs.values();
	}

	public final Collection<NativeCFG> getConstructs() {
		return constructs.values();
	}
	
	public final Collection<CodeMember> getCodeMembers() {
		HashSet<CodeMember> all = new HashSet<>(getCFGs());
		all.addAll(getConstructs());
		return all;
	}

	public final Global getGlobal(String name) {
		return globals.get(name);
	}

	public final CFG getCFG(String signature) {
		return cfgs.get(signature);
	}

	public final NativeCFG getConstruct(String signature) {
		return constructs.get(signature);
	}

	public final Collection<CFG> getCFGsByName(String name) {
		return cfgs.values().stream().filter(c -> c.getDescriptor().getName().equals(name))
				.collect(Collectors.toList());
	}

	public final Collection<NativeCFG> getConstructsByName(String name) {
		return constructs.values().stream().filter(c -> c.getDescriptor().getName().equals(name))
				.collect(Collectors.toList());
	}

	public final Collection<CodeMember> getCodeMembersByName(String name) {
		HashSet<CodeMember> all = new HashSet<>(getCFGsByName(name));
		all.addAll(getConstructsByName(name));
		return all;
	}

	public Collection<CFG> getAllCFGs() {
		return new HashSet<>(getCFGs());
	}

	public Collection<Global> getAllGlobals() {
		return new HashSet<>(getGlobals());
	}
	
	public Collection<NativeCFG> getAllConstructs() {
		return new HashSet<>(getConstructs());
	}
	
	public final Collection<CodeMember> getAllCodeMembers() {
		HashSet<CodeMember> all = new HashSet<>(getAllCFGs());
		all.addAll(getAllConstructs());
		return all;
	}

	public boolean addGlobal(Global global) {
		return globals.putIfAbsent(global.getName(), global) == null;
	}

	public boolean addCFG(CFG cfg) {
		return cfgs.putIfAbsent(cfg.getDescriptor().getSignature(), cfg) == null;
	}

	public boolean addConstruct(NativeCFG construct) {
		return constructs.putIfAbsent(construct.getDescriptor().getSignature(), construct) == null;
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
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
