package org.gama.lang.bean;

/**
 * @author Guillaume Mary
 */
@FunctionalInterface
public interface Converter<I, O, E extends Exception> {
	
	O convert(I input) throws E;
}
