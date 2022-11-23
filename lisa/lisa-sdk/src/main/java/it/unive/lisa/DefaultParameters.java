package it.unive.lisa;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that lets one define default parameters types for analysis
 * components' implementations. Each parameter is specified through it's class
 * object. {@link LiSAFactory#getInstance(Class, Object...)} will be used to
 * generate objects for each specified parameter, and will pass them to a
 * compatible constructor to create the annotated class.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DefaultParameters {

	/**
	 * The array of types of each parameter needed to construct the annotated
	 * type. Each of them will be instantiated by
	 * {@link LiSAFactory#getInstance(Class, Object...)}.
	 * 
	 * @return the types
	 */
	Class<?>[] value() default {};
}
