package org.gama.lang.collection;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.gama.lang.Reflections;
import org.gama.lang.bean.ClassIterator;
import org.gama.lang.bean.InterfaceIterator;
import org.gama.lang.bean.MethodIterator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.gama.lang.test.Assertions.assertEquals;
import static org.gama.lang.test.Assertions.assertThrows;
import static org.gama.lang.test.Assertions.hasExceptionInCauses;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;

/**
 * @author Guillaume Mary
 */
class ReadOnlyListTest {
	
	private static final Set<Method> MUTATING_METHODS = new HashSet<>();
	private static final Set<Method> DEFAULT_METHODS = new HashSet<>();
	private static final Set<Method> EQUALS_HASHCODE_METHODS = new HashSet<>();
	private static final Set<Method> ITERATOR_MUTATING_METHODS = new HashSet<>();
	
	static {
		try {
			MUTATING_METHODS.add(List.class.getMethod("set", int.class, Object.class));
			MUTATING_METHODS.add(List.class.getMethod("add", Object.class));
			MUTATING_METHODS.add(List.class.getMethod("add", int.class, Object.class));
			MUTATING_METHODS.add(List.class.getMethod("remove", int.class));
			MUTATING_METHODS.add(List.class.getMethod("remove", Object.class));
			MUTATING_METHODS.add(List.class.getMethod("addAll", Collection.class));
			MUTATING_METHODS.add(List.class.getMethod("addAll", int.class, Collection.class));
			MUTATING_METHODS.add(List.class.getMethod("removeAll", Collection.class));
			MUTATING_METHODS.add(List.class.getMethod("retainAll", Collection.class));
			MUTATING_METHODS.add(List.class.getMethod("replaceAll", UnaryOperator.class));
			MUTATING_METHODS.add(List.class.getMethod("sort", Comparator.class));
			MUTATING_METHODS.add(List.class.getMethod("clear"));
			// adding overriden methods because class iterator pass on them and they are not equal to the overriding ones
			MUTATING_METHODS.add(Collection.class.getMethod("add", Object.class));
			MUTATING_METHODS.add(Collection.class.getMethod("addAll", Collection.class));
			MUTATING_METHODS.add(Collection.class.getMethod("remove", Object.class));
			MUTATING_METHODS.add(Collection.class.getMethod("removeAll", Collection.class));
			MUTATING_METHODS.add(Collection.class.getMethod("retainAll", Collection.class));
			MUTATING_METHODS.add(Collection.class.getMethod("clear"));
			
			DEFAULT_METHODS.add(Collection.class.getMethod("stream"));
			DEFAULT_METHODS.add(Collection.class.getMethod("parallelStream"));
			DEFAULT_METHODS.add(Collection.class.getMethod("removeIf", Predicate.class));
			DEFAULT_METHODS.add(Iterable.class.getMethod("forEach", Consumer.class));
			
			EQUALS_HASHCODE_METHODS.add(List.class.getMethod("equals", Object.class));
			EQUALS_HASHCODE_METHODS.add(List.class.getMethod("hashCode"));
			EQUALS_HASHCODE_METHODS.add(Collection.class.getMethod("equals", Object.class));
			EQUALS_HASHCODE_METHODS.add(Collection.class.getMethod("hashCode"));
			
			ITERATOR_MUTATING_METHODS.add(ListIterator.class.getMethod("set", Object.class));
			ITERATOR_MUTATING_METHODS.add(ListIterator.class.getMethod("add", Object.class));
			ITERATOR_MUTATING_METHODS.add(ListIterator.class.getMethod("remove"));
			ITERATOR_MUTATING_METHODS.add(Iterator.class.getMethod("remove"));
		} catch (NoSuchMethodException noSuchMethodException) {
			throw new RuntimeException(noSuchMethodException);
		}
	}
	
	@Test
	void methodsInvokeDelegateMethods() {
		List delegate = Mockito.mock(List.class);
		
		ReadOnlyList testInstance = new ReadOnlyList(delegate);
		List<Class> listClassInheritance = Iterables.copy(new InterfaceIterator(new ClassIterator(List.class, null)));
		// List.class must be added because it is not included by inheritance iterator
		listClassInheritance.add(0, List.class);
		MethodIterator methodInHierarchyIterator = new MethodIterator(listClassInheritance.iterator());
		FilteringIterator<Method> nonMutingMethodsIterator = new FilteringIterator<>(methodInHierarchyIterator,
				m -> !MUTATING_METHODS.contains(m) && !EQUALS_HASHCODE_METHODS.contains(m) && !DEFAULT_METHODS.contains(m));
		Iterable<Method> methods = () -> nonMutingMethodsIterator;
		int methodCount = 0;
		for (Method method : methods) {
			Object invokationResult;
			try {
				// we create default arguments otherwise we get IllegalArgumentException from the JVM at invoke() time
				Object[] args = new Object[method.getParameterCount()];
				Class<?>[] parameterTypes = method.getParameterTypes();
				for (int i = 0; i < parameterTypes.length; i++) {
					Class arg = parameterTypes[i];
					if (arg.isArray()) {
						args[i] = Array.newInstance(arg.getComponentType(), 0);
					} else {
						args[i] = Reflections.PRIMITIVE_DEFAULT_VALUES.getOrDefault(arg, null /* default value for any non-primitive Object */);
					}
				}
				invokationResult = method.invoke(testInstance, args);
				
				// hacking some checks because some methods don't redirect to themselves
				switch (method.getName()) {
					case "iterator":
						Mockito.verify(delegate).listIterator(eq(0));
						break;
					case "spliterator":
						Mockito.verifyNoMoreInteractions(delegate);
						break;
					case "listIterator":
						Mockito.verify(delegate).listIterator(eq(0));
						assertTrue(invokationResult instanceof ReadOnlyIterator);
						break;
					default:
						Object delegateResult = method.invoke(Mockito.verify(delegate), args);
						// small hack because Mockito is not consistent with itself : mock() returns an empty instances whereas verify() returns null
						if (method.equals(List.class.getMethod("subList", int.class, int.class))) {
							delegateResult = new ReadOnlyList();
						}
						assertEquals(delegateResult, invokationResult);
						break;
				}
				Mockito.clearInvocations(delegate);
				methodCount++;
			} catch (ReflectiveOperationException | IllegalArgumentException e) {
				throw new RuntimeException("Error executing " + Reflections.toString(method), e);
			}
		}
		// checking that iteration over methods really worked
		Assertions.assertEquals(24, methodCount);
	}
	
	@Test
	void readOnlyMethodsThrowException() {
		List delegate = Mockito.mock(List.class);
		
		ReadOnlyList testInstance = new ReadOnlyList(delegate);
		for (Method method : MUTATING_METHODS) {
			// we create default arguments otherwise we get IllegalArgumentException from the JVM at invoke() time
			Object[] args = new Object[method.getParameterCount()];
			Class<?>[] parameterTypes = method.getParameterTypes();
			for (int i = 0; i < parameterTypes.length; i++) {
				Class arg = parameterTypes[i];
				if (arg.isArray()) {
					args[i] = Array.newInstance(arg.getComponentType(), 0);
				} else {
					args[i] = Reflections.PRIMITIVE_DEFAULT_VALUES.getOrDefault(arg, null /* default value for any non-primitive Object */);
				}
			}
			assertThrows(() -> {
				try {
					method.invoke(testInstance, args);
				} catch (ReflectiveOperationException | IllegalArgumentException e) {
					throw new RuntimeException("Error executing " + Reflections.toString(method), e);
				}
			}, hasExceptionInCauses(UnsupportedOperationException.class));
		}
	}
	
	@Test
	void listIteratorMethodsInvokeDelegateMethods() {
		List list = Mockito.mock(List.class);
		ListIterator delegate = Mockito.mock(ListIterator.class);
		Mockito.when(list.listIterator(eq(0))).thenReturn(delegate);
//		new ReadOnlyList(list).new ReadOnlyListIterator(0) 
//		List testInstance = Mockito.mock(new ReadOnlyList(Mockito.mock(List.class)).listIterator());
		
		ListIterator testInstance = new ReadOnlyList<>(list).listIterator();
		List<Class> listClassInheritance = Iterables.copy(new InterfaceIterator(new ClassIterator(ListIterator.class, null)));
		// ListIterator.class must be added because it is not included by inheritance iterator
		listClassInheritance.add(0, ListIterator.class);
		MethodIterator methodInHierarchyIterator = new MethodIterator(listClassInheritance.iterator());
		FilteringIterator<Method> nonMutingMethodsIterator = new FilteringIterator<>(methodInHierarchyIterator,
				m -> !ITERATOR_MUTATING_METHODS.contains(m) && !EQUALS_HASHCODE_METHODS.contains(m) && !DEFAULT_METHODS.contains(m));
		Iterable<Method> methods = () -> nonMutingMethodsIterator;
		int methodCount = 0;
		for (Method method : methods) {
			Object invokationResult;
			try {
				// we create default arguments otherwise we get IllegalArgumentException from the JVM at invoke() time
				Object[] args = new Object[method.getParameterCount()];
				Class<?>[] parameterTypes = method.getParameterTypes();
				for (int i = 0; i < parameterTypes.length; i++) {
					Class arg = parameterTypes[i];
					if (arg.isArray()) {
						args[i] = Array.newInstance(arg.getComponentType(), 0);
					} else {
						args[i] = Reflections.PRIMITIVE_DEFAULT_VALUES.getOrDefault(arg, null /* default value for any non-primitive Object */);
					}
				}
				invokationResult = method.invoke(testInstance, args);
				
				// hacking some checks because some methods don't redirect to themselves
				Object delegateResult = method.invoke(Mockito.verify(delegate), args);
				// small hack because Mockito is not consistent with itself : mock() returns an empty instances whereas verify() returns null
				if (method.equals(List.class.getMethod("subList", int.class, int.class))) {
					delegateResult = new ReadOnlyList();
				}
				assertEquals(delegateResult, invokationResult);
				Mockito.clearInvocations(delegate);
				methodCount++;
			} catch (ReflectiveOperationException | IllegalArgumentException e) {
				throw new RuntimeException("Error executing " + Reflections.toString(method), e);
			}
		}
		// checking that iteration over methods really worked
		Assertions.assertEquals(9, methodCount);
	}
	
	@Test
	void listIteratorReadOnlyMethodsThrowException() {
		List delegate = Mockito.mock(List.class);
		
		ListIterator testInstance = new ReadOnlyList(delegate).listIterator();
		for (Method method : ITERATOR_MUTATING_METHODS) {
			// we create default arguments otherwise we get IllegalArgumentException from the JVM at invoke() time
			Object[] args = new Object[method.getParameterCount()];
			Class<?>[] parameterTypes = method.getParameterTypes();
			for (int i = 0; i < parameterTypes.length; i++) {
				Class arg = parameterTypes[i];
				if (arg.isArray()) {
					args[i] = Array.newInstance(arg.getComponentType(), 0);
				} else {
					args[i] = Reflections.PRIMITIVE_DEFAULT_VALUES.getOrDefault(arg, null /* default value for any non-primitive Object */);
				}
			}
			assertThrows(() -> {
				try {
					method.invoke(testInstance, args);
				} catch (ReflectiveOperationException | IllegalArgumentException e) {
					throw new RuntimeException("Error executing " + Reflections.toString(method), e);
				}
			}, hasExceptionInCauses(UnsupportedOperationException.class));
		}
	}
	
}