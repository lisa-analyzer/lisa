package it.unive.lisa;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation defining the default implementation for an analysis component.
 * The value of this annotation will be looked up by
 * {@link LiSAFactory#getDefaultFor(Class, Object...)} to instantiate the
 * appropriate class.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultImplementation {

	/**
	 * The class of the default implementation of the annotated analysis
	 * component.
	 * 
	 * @return the default implementation's class
	 */
	Class<?> value();
}
