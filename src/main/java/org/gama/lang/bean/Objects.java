package org.gama.lang.bean;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * @author Guillaume Mary
 */
public class Objects {
	
	/**
	 * Checks that o1.equals(o2) by taking null check into account
	 *
	 * @param o1 an instance, null possible
	 * @param o2 an instance, null possible
	 * @return true if o1 == null && o2 == null
	 *		<br> o1.equals(o2) if o1 and o2 non null
	 *		<br> false if o1 != null or-exclusive o2 != null
	 */
	public static boolean equalsWithNull(Object o1, Object o2) {
		return equalsWithNull(o1, o2, Object::equals);
	}
	
	/**
	 * Checks that 2 instance are equals according a {@link java.util.function.Predicate} and by taking null check into account
	 *
	 * @param t an instance, null possible
	 * @param u an instance, null possible
	 * @return true if o1 == null && o2 == null
	 *		<br> o1.equals(o2) if o1 and o2 non null
	 *		<br> false if o1 != null or-exclusive o2 != null
	 */
	public static <T, U> boolean equalsWithNull(T t, U u, BiPredicate<T, U> equalsNonNullDelegate) {
		return (t == null && u == null)
				|| ((t != null) == (u != null) && equalsNonNullDelegate.test(t, u));
	}
	
	public static String preventNull(String value) {
		return preventNull(value, "");
	}
	
	public static <E> E preventNull(E value, E nullValue) {
		return value == null ? nullValue : value;
	}
	
	/**
	 * Static method to negate the given predicate so one can write {@code not(String::contains)}.
	 * 
	 * @param predicate any {@link Predicate}, a method reference is prefered else this method as no purpose and can be replaced by {@link Predicate#negate}
	 * @param <E> input type of tested elements
	 * @return a negated {@link Predicate} of the given one
	 */
	public static <E> Predicate<E> not(Predicate<E> predicate) {
		return predicate.negate();
	}
}
