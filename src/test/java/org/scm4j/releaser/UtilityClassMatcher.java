package org.scm4j.releaser;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class UtilityClassMatcher extends TypeSafeDiagnosingMatcher<Class<?>> {

	public UtilityClassMatcher() {
	}

	public static UtilityClassMatcher isUtilityClass() {
		return new UtilityClassMatcher();
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("should be declared final");
		description.appendText("\nand have a private default constructor doing nothing");
		description.appendText("\nand have only static methods");
	}

	@Override
	protected boolean matchesSafely(Class<?> item, Description mismatchDescription) {
		if (!Modifier.isFinal(item.getModifiers())) {
			mismatchDescription.appendText("is not declared final");
			return false;
		}
		try {
			Constructor<?> constructor = item.getDeclaredConstructor();
			if (constructor.isAccessible() || !Modifier.isPrivate(constructor.getModifiers())) {
				mismatchDescription.appendText("has not a private default constructor");
				return false;
			}
			constructor.setAccessible(true);
			constructor.newInstance();
		} catch (InvocationTargetException e) {
			if (!(e.getCause() instanceof RuntimeException)) {
				mismatchDescription.appendText("constructor throws checked exceptions");
				return false;
			}
		} catch (ReflectiveOperationException e) {
			mismatchDescription.appendText("fails with " + e.getMessage());
			return false;
		}
		for (Method method : item.getDeclaredMethods()) {
			if (!Modifier.isStatic(method.getModifiers())) {
				mismatchDescription.appendText("has a non static method ").appendValue(method.getName());
				return false;
			}
		}
		return true;
	}

}
