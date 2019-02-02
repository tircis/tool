package org.gama.lang;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Optional;

import org.gama.lang.Reflections.InvokationRuntimeException;
import org.gama.lang.Reflections.MemberNotFoundException;
import org.gama.lang.exception.Exceptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.gama.lang.Reflections.*;
import static org.gama.lang.ThreadLocals.doWithThreadLocal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Guillaume Mary
 */
public class ReflectionsTest {
	
	@Test
	public void testToString() {
		assertEquals("j.l.String", Reflections.toString(String.class));
		Runnable fullPackagePrintAssertion = () -> assertEquals("java.lang.String", Reflections.toString(String.class));
		
		// testing option change with "off"
		doWithThreadLocal(PACKAGES_PRINT_MODE_CONTEXT, () -> Optional.of("off"), fullPackagePrintAssertion);
		// checking that default behavior is back to normal
		assertEquals("j.l.String", Reflections.toString(String.class));
		
		// testing option change with "disable"
		doWithThreadLocal(PACKAGES_PRINT_MODE_CONTEXT, () -> Optional.of("disable"), fullPackagePrintAssertion);
		// checking that default behavior is back to normal
		assertEquals("j.l.String", Reflections.toString(String.class));
		
		// testing option change with "false"
		doWithThreadLocal(PACKAGES_PRINT_MODE_CONTEXT, () -> Optional.of("false"), fullPackagePrintAssertion);
		// checking that default behavior is back to normal
		assertEquals("j.l.String", Reflections.toString(String.class));
	}
	
	
	@Test
	public void testGetDefaultConstructor() {
		Constructor<Toto> defaultConstructor = getDefaultConstructor(Toto.class);
		assertNotNull(defaultConstructor);
	}
	
	@Test
	public void testGetDefaultConstructor_closedClass() {
		Constructor<ClosedClass> defaultConstructor = getDefaultConstructor(ClosedClass.class);
		assertNotNull(defaultConstructor);
	}
	
	@Test
	public void testGetDefaultConstructor_innerStaticClass() {
		Constructor<InnerStaticClass> defaultConstructor = getDefaultConstructor(InnerStaticClass.class);
		assertNotNull(defaultConstructor);
	}
	
	@Test
	public void testGetDefaultConstructor_abstractClass() {
		Constructor<AbstractMap> defaultConstructor = getDefaultConstructor(AbstractMap.class);
		assertNotNull(defaultConstructor);
	}
	
	
	public static Object[][] testGetDefaultConstructor_throwingCases_data() {
		return new Object[][] {
				{ InnerClass.class, "Class o.g.l.InnerClass has no default constructor because it is an inner non static class" +
						" (needs an instance of the encosing class to be constructed)" },
				{ int.class, "Class int has no default constructor because it is a primitive type" },
				{ int[].class, "Class int[] has no default constructor because it is an array" },
				{ CharSequence.class, "Class j.l.CharSequence has no default constructor because it is an interface" },
				{ URL.class, "Class j.n.URL has no default constructor" }
		};
	}
	
	@ParameterizedTest
	@MethodSource("testGetDefaultConstructor_throwingCases_data")
	public void testGetDefaultConstructor_throwingCases(Class clazz, String expectedMessage) {
		assertEquals(expectedMessage,
				assertThrows(UnsupportedOperationException.class, () -> getDefaultConstructor(clazz)).getMessage());
	}
	
	@Test
	public void testGetConstructor_private() {
		Constructor<InnerStaticClassWithPrivateConstructor> constructor1 = getConstructor(InnerStaticClassWithPrivateConstructor.class);
		assertNotNull(constructor1);
		Constructor<InnerClassWithPrivateConstructor> constructor2 = getConstructor(InnerClassWithPrivateConstructor.class, ReflectionsTest.class);
		assertNotNull(constructor2);
	}
	
	@Test
	public void testGetConstructor_innerNonStaticClass_missingEnclosingClassAsParameter_throwsException() {
		assertEquals("Non static inner classes require an enclosing class parameter as first argument",
				assertThrows(MemberNotFoundException.class, () -> getConstructor(InnerClassWithPrivateConstructor.class)).getMessage());
	}
	
	public static Object[][] testGetFieldData() {
		return new Object[][] {
				{ Toto.class, "a", Toto.class },
				{ Toto.class, "b", Toto.class },
				// inheritance test
				{ Tutu.class, "a", Toto.class },
				{ Tutu.class, "b", Tata.class },
		};
	}
	
	@ParameterizedTest
	@MethodSource("testGetFieldData")
	public void testGetField(Class<Toto> fieldClass, String fieldName, Class expectedDeclaringClass) {
		Field field = findField(fieldClass, fieldName);
		assertNotNull(field);
		assertEquals(fieldName, field.getName());
		assertEquals(expectedDeclaringClass, field.getDeclaringClass());
	}
	
	public static Object[][] testGetMethodData() {
		return new Object[][] {
				{ Toto.class, "toto", null, Toto.class, 0 },
				{ Toto.class, "toto2", null, Toto.class, 0 },
				// with parameter
				{ Toto.class, "toto", Integer.TYPE, Toto.class, 1 },
				{ Toto.class, "toto2", Integer.TYPE, Toto.class, 1 },
				// inheritance test
				{ Tutu.class, "toto", null, Toto.class, 0 },
				{ Tutu.class, "toto2", null, Toto.class, 0 },
				{ Tutu.class, "toto", Integer.TYPE, Toto.class, 1 },
				{ Tutu.class, "toto2", Integer.TYPE, Toto.class, 1 },
		};
	}
	
	@ParameterizedTest
	@MethodSource("testGetMethodData")
	public void testGetMethod(Class<Toto> methodClass, String methodName, Class parameterType, Class expectedDeclaringClass, int exectedParameterCount) {
		Method method;
		if (parameterType == null) {
			method = findMethod(methodClass, methodName);
		} else {
			method = findMethod(methodClass, methodName, parameterType);
		}
		assertNotNull(method);
		assertEquals(methodName, method.getName());
		assertEquals(expectedDeclaringClass, method.getDeclaringClass());
		assertEquals(exectedParameterCount, method.getParameterTypes().length);
	}
	
	@Test
	public void testGetConstructor() throws NoSuchMethodException {
		assertEquals(String.class.getConstructor(String.class), getConstructor(String.class, String.class));
		assertThrows(MemberNotFoundException.class, () -> getConstructor(String.class, Reflections.class));
	}
	
	@Test
	public void testFindConstructor() throws NoSuchMethodException {
		assertEquals(String.class.getConstructor(String.class), findConstructor(String.class, String.class));
		assertNull(findConstructor(String.class, Reflections.class));
	}
	
	@Test
	public void testOnJavaBeanPropertyWrapperName_getterIsRecognized() throws NoSuchMethodException {
		class X {
			void getA() {
			}
		}
		boolean found = onJavaBeanPropertyWrapperName(X.class.getDeclaredMethod("getA"), m -> true, m -> false, m -> false);
		assertTrue(found);
	}
	
	@Test
	public void testOnJavaBeanPropertyWrapperName_getterIsRecognized_boolean() throws NoSuchMethodException {
		class X {
			void isA() {
			}
		}
		boolean found = onJavaBeanPropertyWrapperName(X.class.getDeclaredMethod("isA"), m -> false, m -> false, m -> true);
		assertTrue(found);
	}
	
	@Test
	public void testOnJavaBeanPropertyWrapperName_setterIsRecognized() throws NoSuchMethodException {
		class X {
			void setA() {
			}
		}
		boolean found = onJavaBeanPropertyWrapperName(X.class.getDeclaredMethod("setA"), m -> false, m -> true, m -> false);
		assertTrue(found);
	}
	
	@Test
	public void testOnJavaBeanPropertyWrapper_getterIsRecognized() throws NoSuchMethodException {
		class X {
			String getA() {
				return null;
			}
		}
		boolean found = onJavaBeanPropertyWrapper(X.class.getDeclaredMethod("getA"), m -> true, m -> false, m -> false);
		assertTrue(found);
	}
	
	@Test
	public void testOnJavaBeanPropertyWrapper_getterIsRecognized_boolean() throws NoSuchMethodException {
		class X {
			boolean isA() {
				return true;
			}
		}
		boolean found = onJavaBeanPropertyWrapper(X.class.getDeclaredMethod("isA"), m -> false, m -> false, m -> true);
		assertTrue(found);
	}
	
	@Test
	public void testOnJavaBeanPropertyWrapper_setterIsRecognized() throws NoSuchMethodException {
		class X {
			void setA(String a) {
			}
		}
		boolean found = onJavaBeanPropertyWrapper(X.class.getDeclaredMethod("setA", String.class), m -> false, m -> true, m -> false);
		assertTrue(found);
	}
	
	@Test
	public void testNewInstance_privateConstructor() {
		// This shouldn't cause problem
		ClosedClass closedClass = newInstance(ClosedClass.class);
		// minimal test
		assertNotNull(closedClass);
	}
	
	@Test
	public void testNewInstance_interface_throwsException() {
		InvokationRuntimeException thrownException = assertThrows(InvokationRuntimeException.class,
				() -> newInstance(CharSequence.class));
		assertNotNull(Exceptions.findExceptionInHierarchy(thrownException, UnsupportedOperationException.class,
				"Class j.l.CharSequence has no default constructor because it is an interface"));
	}
	
	@Test
	public void testNewInstance_constructorThrowsException_throwsException() {
		InvokationRuntimeException thrownException = assertThrows(InvokationRuntimeException.class,
				() -> newInstance(ThrowingConstructorClass.class));
		
		assertNotNull(Exceptions.findExceptionInHierarchy(thrownException, InvokationRuntimeException.class,
				"Class org.gama.lang.ReflectionsTest$ThrowingConstructorClass can't be instanciated"),
				() -> {
					StringWriter exceptionAsString = new StringWriter();
					thrownException.printStackTrace(new PrintWriter(exceptionAsString));
					return "Can't find exception in " + exceptionAsString;
				});
		// looking for exception thrown by constructor
		assertNotNull(Exceptions.findExceptionInHierarchy(thrownException, NullPointerException.class));
	}
	
	@Test
	public void testInvoke() throws NoSuchMethodException {
		Toto toto = new Toto();
		Method setter = Toto.class.getMethod("setA", int.class);
		
		// minimal test
		invoke(setter, toto, 42);
		assertEquals(42, toto.getA());
		
		// wrong input type (throws IllegalArgumentException)
		assertThrows(InvokationRuntimeException.class, () -> invoke(setter, toto, "dummyString"));
		
		// Non accessible method
		Method privateMethod = Toto.class.getDeclaredMethod("toto");
		assertThrows(InvokationRuntimeException.class, () -> invoke(privateMethod, toto));
	}
	
	@Test
	public void testWrappedField() throws NoSuchMethodException, NoSuchFieldException {
		// simple case
		Field field = wrappedField(Toto.class.getDeclaredMethod("setB", String.class));
		assertEquals(Toto.class.getDeclaredField("b"), field);
		
		// Tata.b hides Toto.b
		field = wrappedField(Tata.class.getDeclaredMethod("setB", String.class));
		assertEquals(Tata.class.getDeclaredField("b"), field);
		
		// true override : Tata.setA overrides Toto.setA, field is in Toto
		field = wrappedField(Tata.class.getDeclaredMethod("setA", Integer.TYPE));
		assertEquals(Toto.class.getDeclaredField("a"), field);
	}
	
	@Test
	public void testPropertyName() throws NoSuchMethodException {
		// simple case
		String propertyName = propertyName(Toto.class.getDeclaredMethod("setB", String.class));
		assertEquals("b", propertyName);
	}
	
	@Test
	public void testPropertyName_methodDoesntFitJavaBeanConvention_exceptionIsThrown() throws NoSuchMethodException {
		Method fixBMethod = Toto.class.getDeclaredMethod("fixB", String.class);
		assertEquals("Field wrapper void o.g.l.Toto.fixB(j.l.String) doesn't fit encapsulation naming convention",
				assertThrows(MemberNotFoundException.class, () -> propertyName(fixBMethod)).getMessage());
	}
	
	@Test
	public void testForName() throws ClassNotFoundException {
		assertEquals(boolean.class, forName("Z"));
		assertEquals(int.class, forName("I"));
		assertEquals(long.class, forName("J"));
		assertEquals(short.class, forName("S"));
		assertEquals(byte.class, forName("B"));
		assertEquals(double.class, forName("D"));
		assertEquals(float.class, forName("F"));
		assertEquals(char.class, forName("C"));
		assertEquals(void.class, forName("V"));
		assertEquals(String.class, forName(String.class.getName()));
		assertEquals(Object[].class, forName("[Ljava.lang.Object;"));
		assertEquals(Object.class, forName("java.lang.Object"));
		assertEquals(boolean[].class, forName("[Z"));
		assertEquals(boolean[][].class, forName("[[Z"));
	}
	
	private static class ClosedClass {
		private ClosedClass() {
		}
	}
	
	private static class Toto {
		private int a;
		private String b;
		
		private void toto() {
		}
		
		private void toto(int a) {
		}
		
		// Method toto2() is declared in reverse order thant toto() ones to test Reflections on JDK robustness
		// (open jdk doesn't return then in same order)
		private void toto2(int a) {
		}
		
		private void toto2() {
		}
		
		public int getA() {
			return a;
		}
		
		public void setA(int a) {
			this.a = a;
		}
		
		public void setB(String b) {
			this.b = b;
		}
		
		/** Non conventional setter */
		public void fixB(String b) {
			setB(b);
		}
	}
	
	private static class Tata extends Toto {
		private String b;
		
		@Override
		public void setA(int a) {
			super.setA(a);
		}
		
		@Override
		public void setB(String b) {
			this.b = b;
		}
	}
	
	private static class Titi extends Tata {
		// no field, no method, for no member traversal check
	}
	
	private static class Tutu extends Titi {
		// no field, no method, for no member traversal check
	}
	
	private class InnerClass {
		
	}
	
	private class InnerClassWithPrivateConstructor {
		
		private InnerClassWithPrivateConstructor() {
		}
	}
	
	private static class InnerStaticClass {
		
	}
	
	private static class InnerStaticClassWithPrivateConstructor {
		
		private InnerStaticClassWithPrivateConstructor() {
			
		}
	}
	
	private static class ThrowingConstructorClass {
		
		public ThrowingConstructorClass() {
			throw new NullPointerException();
		}
	}
}