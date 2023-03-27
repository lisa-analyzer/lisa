package it.unive.lisa.conf;

import it.unive.lisa.LiSA;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * A holder for the configuration of a {@link LiSA} analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class BaseConfiguration {

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		try {
			for (Field field : getClass().getFields())
				if (!Modifier.isStatic(field.getModifiers()))
					result = prime * result + Objects.hashCode(field.get(this));
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("Cannot access one of this class' public fields", e);
		}
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		try {
			for (Field field : getClass().getFields())
				if (!Modifier.isStatic(field.getModifiers())) {
					Object value = field.get(this);
					Object ovalue = field.get(obj);
					if (!Objects.equals(value, ovalue))
						return false;
				}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("Cannot access one of this class' public fields", e);
		}
		return true;
	}
}