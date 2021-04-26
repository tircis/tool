package org.gama.lang.collection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * @author Guillaume Mary
 */
public class Collections {
	
	/**
	 * Returns true if the Collection is null or empty
	 * 
	 * @param c a Collection
	 * @return true if the Collection is null or empty
	 */
	public static boolean isEmpty(Collection c) {
		return c == null || c.isEmpty();
	}
	
	/**
	 * Returns true if the Map is null or empty
	 *
	 * @param m a Map
	 * @return true if the Map is null or empty
	 */
	public static boolean isEmpty(Map m) {
		return m == null || m.isEmpty();
	}
	
	/**
	 * Creates a {@link Set} based on object identity for its comparison
	 * @param <E> element type
	 * @return a new {@link Set} based on object identity for its comparison
	 */
	public static <E> Set<E> newIdentitySet() {
		return java.util.Collections.newSetFromMap(new IdentityHashMap<>());
	}
	
	/**
	 * Creates a {@link Set} based on object identity for its comparison
	 * 
	 * @param initialCapacity the wanted initial capacity of the {@link Set}
	 * @param <E> element type
	 * @return a new {@link Set} based on object identity for its comparison
	 */
	public static <E> Set<E> newIdentitySet(int initialCapacity) {
		return java.util.Collections.newSetFromMap(new IdentityHashMap<>(initialCapacity));
	}
	
	/**
	 * Create a Last-In-Firt-Out {@link Queue}
	 * 
	 * @param <E> element type
	 * @return a new Last-In-Firt-Out {@link Queue}
	 */
	public static <E> Queue<E> newLifoQueue() {
		return java.util.Collections.asLifoQueue(new ArrayDeque<>());
	}
	
	/**
	 * Same as {@link java.util.Collections#addAll(Collection, Object[])} but with Collection return type
	 * @param c a (non null) collection
	 * @param elements elements to be added to the collection
	 * @param <T> Element type
	 * @param <C> Collection type
	 * @return c
	 */
	public static <T, C extends Collection<? super T>> C addAll(C c, T... elements) {
		c.addAll(Arrays.asList(elements));
		return c;
	}
	
	public static <E> List<E> cat(Collection<E>... collections) {
		List<E> toReturn = new ArrayList<>(collections.length * 10);    // arbitrary size, ArrayList.addAll will adapt
		for (Collection<E> collection : collections) {
			toReturn.addAll(collection);
		}
		return toReturn;
	}
	
	public static <E> List<E> cutHead(List<E> list) {
		return cutHead(list, 1);
	}
	
	public static <E> List<E> cutHead(List<E> list, int elementCount) {
		return new ArrayList<>(list.subList(elementCount, list.size()));
	}
	
	public static <E> List<E> cutTail(List<E> list) {
		return cutTail(list, 1);
	}
	
	public static <E> List<E> cutTail(List<E> list, int elementCount) {
		return new ArrayList<>(list.subList(0, list.size() - elementCount));
	}
	
	/**
	 * Parcels a Collection into pieces.
	 * 
	 * @param data an Iterable
	 * @param blockSize the size of blocks to be created (the last will contain remaining elements)
	 * @return a List of blocks of elements
	 */
	public static <E> List<List<E>> parcel(Iterable<E> data, int blockSize) {
		final List<List<E>> blocks = new ArrayList<>();
		// on s'assure d'avoir une liste pour permettre l'utilisation de subList ensuite
		List<E> dataAsList = asList(data);
		int i = 0;
		int dataSize = dataAsList.size();
		int blockCount = dataSize / blockSize;
		// if some remain, an additional block must be created
		if (dataSize % blockSize != 0) {
			blockCount++;
		}
		// parcelling
		while (i < blockCount) {
			blocks.add(new ArrayList<>(dataAsList.subList(i * blockSize, Math.min(dataSize, ++i * blockSize))));
		}
		return blocks;
	}
	
	/**
	 * Returns a Iterable as a List: return it if it's one, else create a new one from it.
	 * 
	 * @param c a Iterable
	 * @param <E> contained element type
	 * @return a new List or the Collection if it's one
	 */
	public static <E> List<E> asList(Iterable<E> c) {
		return c instanceof List ? (List<E>) c : Iterables.copy(c);
	}
}
