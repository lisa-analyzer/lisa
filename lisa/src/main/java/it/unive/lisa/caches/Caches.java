package it.unive.lisa.caches;

import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.util.collections.ExternalSetCache;

public class Caches {
	
	private static final ExternalSetCache<Type> types = new ExternalSetCache<>();
	
	public static ExternalSetCache<Type> types() {
		return types;
	}
}
