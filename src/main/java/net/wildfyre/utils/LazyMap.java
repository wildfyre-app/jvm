/*
 * Copyright 2018 Wildfyre.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.wildfyre.utils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents a Map object that is lazily-instantiated. This class is a wrapper around an underlying Map.
 * <p>The basic idea is that, if the map is not initialized, it is considered empty.</p>
 * @param <K> the type of the keys
 * @param <V> the type of the values
 */
public class LazyMap<K, V> implements Map<K, V> {

    //region Attributes

    private Map<K, V> map;

    //endregion
    //region Constructors

    /**
     * Creates an empty object.
     * <p>The default capacity of the Map created by this constructor is voluntarily unspecified, as it may depend on
     * how the underlying map is instantiated, later in the life of this object. If the capacity is important to you,
     * use {@link #LazyMap(int)} instead.</p>
     */
    public LazyMap(){
        // Nothing to do because of lazy-initialization
    }

    /**
     * Creates an empty lazy map of specified capacity.
     * <p>In this case, the underlying map object is a HashMap, see {@link java.util.HashMap#HashMap(int)}.</p>
     * @param size the capacity of the underlying map object.
     */
    public LazyMap(int size){
        map = new HashMap<>(size);
    }

    /**
     * Creates a lazy map from the given map. Note that no copying is involved, the LazyMap's internal map will
     * merely point to the provided reference (which makes this call really performance & memory-efficient, but might
     * cause problems if you apply modifications on the given map).
     * @param map the map object that will be used as underlying map.
     */
    public LazyMap(Map<K, V> map){
        if(map == null)
            throw new NullPointerException("The parameter 'map' cannot be null.");

        this.map = map;
    }

    /**
     * Creates a lazy map from a given collection. This constructor assumes that the key of the Map can be retrieved
     * from the values. This is especially useful in cases where the LazyMap is used to store some data that has an ID.
     * <p>Here is an example of call, assuming we want to store Users, indexed by their ID:</p>
     * <pre>{@code
     * Collection<User> users = ...
     * LazyMap<Integer, User> map = new LazyMap<>(users, u -> u.getID());
     * }</pre>
     * @param objects the values that will be stored in the map
     * @param IDMapper the way to get the key from an object
     */
    public LazyMap(Collection<V> objects, Function<V, K> IDMapper){
        this( objects
            .stream()
            .collect(Collectors.toMap(IDMapper, o -> o))
        );
    }

    //endregion
    //region Utility methods

    /**
     * Is this object initialized?
     * @return {@code true} if this object is initialized.
     */
    protected boolean isInitialized(){
        return map != null;
    }

    /**
     * Specifies that, in the case where this object is not initialized, it should be initialized now before further
     * exploitation.
     */
    protected void requireInitialized(){
        if(!isInitialized())
            map = new HashMap<>();
    }

    //endregion
    //region Map interface implementation

    @Override
    public int size() {
        return isInitialized() ? map.size() : 0;
    }

    @Override
    public boolean isEmpty() {
        return !isInitialized() || map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return isInitialized() && map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return isInitialized() && map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return isInitialized() ? map.get(key) : null;
    }

    @Override
    public V put(K key, V value) {
        requireInitialized();

        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        // The contract of 'remove' specifies that the method should return 'null' if the object was not found.
        // If the map is not initialized, it is unneeded to initialize it, as it is not possible that the object will
        // be found. So we just return null, as this is what would happen if we were to initialize the object then call
        // the method anyway. This way, we respect the contract without needing to initialize the object.

        // If the result is that the map becomes empty, the map is uninitiated to free memory.

        if(isInitialized()) {
            V value = map.remove(key);

            if(map.isEmpty())   // If the resulting map is empty, destroy the object to free memory.
                map = null;

            return value;
        } else return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if(!isInitialized())
            map = new HashMap<>(m);
        else
            map.putAll(m);
    }

    @Override
    public void clear() {
        map = null; // simply destroy the object
    }

    @Override
    public Set<K> keySet() {
        return isInitialized() ? map.keySet() : Collections.emptySet();
    }

    @Override
    public Collection<V> values() {
        return isInitialized() ? map.values() : Collections.emptyList();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return isInitialized() ? map.entrySet() : Collections.emptySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LazyMap<?, ?> lazyMap = (LazyMap<?, ?>) o;
        return Objects.equals(map, lazyMap.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }

    //endregion

}
