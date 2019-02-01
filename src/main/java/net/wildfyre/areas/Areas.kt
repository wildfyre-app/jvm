/*
 * Copyright 2019 Wildfyre.net
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

package net.wildfyre.areas

import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.JsonValue
import com.eclipsesource.json.WriterConfig
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import net.wildfyre.api.Internal
import net.wildfyre.descriptors.CacheManager
import net.wildfyre.http.Method
import net.wildfyre.http.Request
import net.wildfyre.utils.LazyMap
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.set

/**
 * This class is a singleton that represents the available areas.
 * @see net.wildfyre.utils.LazyMap Implementation details
 */
@SuppressFBWarnings(
    value = ["ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD"],
    justification = "FindBugs displays this warning because it doesn't understand Kotlin's objects."
)
object Areas {

    internal var areas: MutableMap<String, Area> = HashMap(0)

    private val manager = CacheManager().setExpirationTime((1000 * 60 * 60).toLong()) // 1 hour

    /**
     * Initializes Areas by calling [load], [Area.loadDrafts] and [Area.loadOwnPosts].
     *
     * @throws Request.CantConnectException if the server cannot connect to the API.
     */
    @Throws(Request.CantConnectException::class)
    fun init() {
        load()

        collection().forEach {
            it.loadDrafts()
            it.loadOwnPosts()
        }
    }

    /**
     * Loads the list of areas.
     *
     * This method is executed in the current thread.
     *
     * @throws Request.CantConnectException if the API cannot connect to the server
     */
    @Throws(Request.CantConnectException::class)
    fun load() {
        val json = Request(Method.GET, "/areas/")
                .addToken(Internal.token())
                .getJsonArray()

        areas = LazyMap(json.size()) // exact number of areas for better memory optimization

        json.forEach { area: JsonValue ->
            val it = area as JsonObject
            val name = it["name"]?.asString()
                ?: throw NullPointerException("Cannot have a 'null' area:\n" + json.toString(WriterConfig.PRETTY_PRINT))
            val displayName = it["displayname"]?.asString()

            areas[name] = get(name).orElseGet { Area(name, displayName) }
        }
    }

    /**
     * Gets the Area from its ID.
     *
     * This API is crafted so you never need IDs, so you probably don't need this
     * method.
     *
     * @param areaID the ID of the requested area.
     * @return The Area corresponding to the given ID, or an empty optional.
     */
    operator fun get(areaID: String): Optional<Area> {
        return Optional.ofNullable(areas[areaID])
    }

    /**
     * Gets the list of areas.
     *
     * The returned collection is read-only, and cannot be modified in any way.
     *
     * @return The list of areas.
     */
    fun collection(): Collection<Area> {
        return Collections.unmodifiableCollection(areas.values)
    }

    /**
     * Removes the loaded areas.
     */
    fun clear() {
        areas.clear()
    }

    /**
     * The cache manager that is responsible for all Areas.
     * @return The Area Cache Manager.
     */
    fun cacheManager(): CacheManager {
        return manager
    }

    /**
     * Removes the areas that are not valid anymore.
     *
     * @see .cacheManager
     */
    fun clean() {
        val time = System.currentTimeMillis()

        areas.values.removeIf { !it.isValid(time) }
    }
}
