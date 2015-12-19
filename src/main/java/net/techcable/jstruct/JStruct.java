package net.techcable.jstruct;

/**
 * All classes annotated with this field will be <href a=https://wikipedia.org/wiki/PassByValue>pass by value</href>
 * <p/>
 * In order to preserve the contract of objects in the java language, JStructs can only have final fields.
 * Otherwise modifications to a JStruct would not be reflected in all objects.
 * <p/>
 * Certain operations can't be performed on a JStruct and will throw an {@link UnsupportedOperationException}:
 * <ul>
 *     <li>Synchronization</li>
 *     <li>wait() and notify()</li>
 *     <li>Identity Comparison (o1 == o2)</li>
 * </ul>
 * <p/>
 * If your JVM does not support escape analysis {@code JStruct} should <b>not</b> be used.
 * Otherwise memory usage will skyrocket, as every struct will be re-allocated on the heap for every method call.
 */
public @interface JStruct {}
