package it.unive.lisa;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that lets one define default implementations for analysis
 * components. {@link LiSAFactory#getDefaultFor(Class, Object...)} will search
 * for subtypes of the given class that are annotated with this annotation to
 * create compatible implementations. If none is found, {@link LiSAFactory} will
 * instead search for types annotated as {@link FallbackImplementation}, an
 * annotation reserved for LiSA to define its own defaults.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DefaultImplementation {
}
