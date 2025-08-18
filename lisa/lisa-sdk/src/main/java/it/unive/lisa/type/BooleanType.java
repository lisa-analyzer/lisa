package it.unive.lisa.type;

/**
 * Boolean type interface. Any concrete Boolean type or Boolean sub-interface
 * should implement/extend this interface.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public interface BooleanType extends Type {

	@Override
	default boolean castIsConversion() {
		return true;
	}
}
