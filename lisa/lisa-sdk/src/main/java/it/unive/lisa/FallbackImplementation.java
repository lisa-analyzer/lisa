package it.unive.lisa;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation reserved for LiSA to define its own default implementations for
 * analysis components. {@link LiSAFactory#getDefaultFor(Class, Object...)} will
 * search for subtypes of the given class that are annotated with
 * {@link DefaultImplementation} to create compatible implementations. If none
 * is found, {@link LiSAFactory} will instead search for types annotated with
 * this annotation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FallbackImplementation {
}
