package it.unive.lisa.cfg.type;

/**
 * The Null type, that is the type of {#link NullLiteral}.
 * 
 * It implements the singleton design pattern, that is 
 * the instances of this type are unique. The unique instance of
 * this type can be retrieved by {@link NullType#INSTANCE}.
 *  
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class NullType implements Type {
	
	/**
	 * Unique instance of {@link NullType}. 
	 */
	public static final NullType INSTANCE = new NullType();
	
	private NullType() {}

	@Override
	public String toString() {
		return "null";
	}
}
