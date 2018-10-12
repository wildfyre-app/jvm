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

package net.wildfyre.areas;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;
import net.wildfyre.api.Internal;
import net.wildfyre.descriptors.CacheManager;
import net.wildfyre.http.IssueInTransferException;
import net.wildfyre.http.Method;
import net.wildfyre.http.Request;
import net.wildfyre.utils.LazyMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This class is a singleton that represents the available areas.
 * @see net.wildfyre.utils.LazyMap Implementation details
 */
public class Areas {

    //region Attributes

    static Map<String, Area> areas = Collections.emptyMap(); // no NullPointerException

    //endregion
    //region Constructors

    /**
     * Private constructor so nobody calls it (= singleton, this class is fully static).
     */
    private Areas(){}

    //endregion
    //region Queries

    /**
     * Initializes Areas by calling {@link #load()}, {@link Area#loadDrafts()} and {@link Area#loadOwnPosts()}.
     *
     * @throws Request.CantConnectException if the server cannot connect to the API.
     */
    public static void init() throws Request.CantConnectException {
        load();

        for(Area a : collection()){
            a.loadDrafts();
            a.loadOwnPosts();
        }
    }

    /**
     * Loads the list of areas.
     *
     * <p>This method is executed in the current thread.</p>
     *
     * @throws Request.CantConnectException if the API cannot connect to the server
     */
    public static void load() throws Request.CantConnectException {
        JsonArray json;
        try {
            json = new Request(Method.GET, "/areas/")
                .addToken(Internal.token())
                .getJson()
                .asArray();

        } catch (IssueInTransferException e) {
            throw new RuntimeException("There was an unforeseen problem with the loading of areas.", e);
        }

        areas = new LazyMap<>(json.size()); // exact number of areas for better memory optimization

        json.forEach((JsonValue area) -> {
            JsonObject it = area.asObject();
            String name = it.getString("name", null);
            String displayName = it.getString("displayname", null);

            if(name == null)
                throw new NullPointerException("Cannot have a 'null' area: \n"
                    + json.toString(WriterConfig.PRETTY_PRINT));

            areas.put (
                name,
                get(name).orElseGet(() -> new Area(name, displayName))
            );
        });
    }

    /**
     * Gets the Area that has the given String as an ID.
     *
     * <p>This method is written to use IDs. This API is crafted so you never need IDs, so you probably don't need this
     * method.</p>
     *
     * @param areaID the ID of the requested area.
     * @return The Area corresponding to the given ID, or an empty optional.
     */
    public static Optional<Area> get(String areaID){
        return Optional.ofNullable(areas.get(areaID));
    }

    /**
     * Gets the list of areas.
     *
     * <p>The returned collection is read-only, and cannot be modified in any way.</p>
     *
     * @return The list of areas.
     * @see #stream() Idem as a Stream, less object instantiation
     */
    public static Collection<Area> collection(){
        return Collections.unmodifiableCollection( areas.values() );
    }

    /**
     * Gets a sequential stream over the areas.
     * @return A stream over the areas.
     * @see #collection() Idem as a Collection
     */
    public static Stream<Area> stream(){
        return areas.values().stream();
    }

    /**
     * Removes the loaded areas.
     */
    public static void clear(){
        areas.clear();
    }

    //endregion
    //region Caching

    private final static CacheManager manager = new CacheManager().setExpirationTime(1000*60*60); // 1 hour

    /**
     * The cache manager that is responsible for all Areas.
     * @return The Area Cache Manager.
     */
    public static CacheManager cacheManager(){
        return manager;
    }

    /**
     * Removes the areas that are not valid anymore.
     *
     * @see #cacheManager() Modify the Cache Manager.
     */
    public static void clean(){
        long time = System.currentTimeMillis();

        areas.values().removeIf(a -> !a.isValid(time));
    }

    //endregion
}
