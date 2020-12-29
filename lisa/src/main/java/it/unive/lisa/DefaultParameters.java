package it.unive.lisa;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation defining the default parameters to use when automatically
 * creating an instance of the annotated type. The value of this annotation will
 * be looked up by {@link LiSAFactory#getInstance(Class, Object...)} if no
 * explicit parameters are specified.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultParameters {

	/**
	 * The default classes of each parameter. Instances of those classes will be
	 * created through {@link LiSAFactory#getInstance(Class, Object...)},
	 * recursively looking for default parameters.
	 * 
	 * @return the classes of each default parameter
	 */
	Class<?>[] value();
}
