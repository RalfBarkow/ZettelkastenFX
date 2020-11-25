package de.danielluedecke.zettelkasten;

import java.lang.reflect.Field;

/**
 * This class offers convenience access testable Zettelkasten objects.
 * 
 * @author Timm Heuss
 *
 */
public class TestObjectFactory {

	/**
	 * Implementation of a thread safe, synchronized singleton pattern
	 * @see <a href="https://web.archive.org/web/20150910003303/http://de.wikibooks.org/wiki/Java_Standard:_Muster_Singleton">Java Standard: Muster Singleton [archived copy]</a>
	 */
	private static TestObjectFactory instance;

	public static synchronized TestObjectFactory getInstance() throws Exception {
		if (TestObjectFactory.instance == null) {
			TestObjectFactory.instance = new TestObjectFactory();
		}
		return TestObjectFactory.instance;
	}

	private TestObjectFactory() {

	}
	
	/**
	 * Helper to retrieve a private / protected field.
	 */
	public static Object getPrivateField(Object instance, String fieldName)
			throws Exception {
		Field field = instance.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(instance);
	}
}
