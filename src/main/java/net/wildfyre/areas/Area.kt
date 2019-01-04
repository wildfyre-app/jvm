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

package net.wildfyre.areas

import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.WriterConfig
import net.wildfyre.api.Internal
import net.wildfyre.descriptors.CacheManager
import net.wildfyre.descriptors.Descriptor
import net.wildfyre.descriptors.NoSuchEntityException
import net.wildfyre.http.IssueInTransferException
import net.wildfyre.http.Method.GET
import net.wildfyre.http.Request
import net.wildfyre.posts.Draft
import net.wildfyre.posts.Post
import net.wildfyre.utils.LazyMap
import net.wildfyre.utils.ProgrammingException
import java.util.*

/**
 * This class represents an Area.
 *
 * Areas are a way Posts are 'grouped'.
 *
 * Each user's spread and reputation is different from one Area to another. The reputation goes up and down when you
 * do certain actions. The spread determines how many people can see your Posts, it cannot be lower than 4.
 *
 * @param id The ID of the Area
 * @param displayName The name of the Area (defaults to the ID)
 */
class Area internal constructor (
    id: String,
    displayName: String?
) : Descriptor() {

    /**
     * The ID of this Area, which looks like its name in all lowercase characters.
     *
     * This API is written so you never *need* to use IDs; so if you are not sure why this exists, you can safely
     * ignore it.
     */
    // Even though it's contrary to convention,
    // it's convenient that this field begins with an uppercase.
    @Suppress("PropertyName")
    val ID: String = id

    /**
     * The name of this Area.
     */
    // If no displayName was specified, copies the ID of the Area
    var name: String = displayName ?: id
        private set

    /**
     * The reputation of the logged-in user, in the current area.
     *
     * The reputation increases and decreases depending on your overall actions on WildFyre.
     * You cannot control it.
     */
    val reputation: Int
        get() {
            if(_reputation == -1)
                update()
            return _reputation
        }
    private var _reputation: Int = -1

    /**
     * The spread of the logged-in user, in the current area.
     *
     * Your spread is related to your reputation, but cannot be inferior to 4.
     * You cannot control it.
     */
    val spread: Int
        get() {
            if(_spread == -1)
                update()
            return _spread
        }
    private var _spread: Int = -1

    private val posts = LazyMap<Long, Post>()

    private val drafts: MutableMap<Long, Draft> = LazyMap()

    private var ownPostsIDs = emptyList<Long>()

    override fun cacheManager(): CacheManager {
        return Areas.cacheManager()
    }

    @Throws(Request.CantConnectException::class, NoSuchEntityException::class)
    override fun update() {
        try {
            val json = Request(GET, "/areas/$ID/rep/")
                .addToken(Internal.token())
                .getJson()
                .asObject()

            _reputation = json["reputation"]?.asInt()
                ?: throw ProgrammingException("Missing reputation.\n" + json.toString(WriterConfig.PRETTY_PRINT))

            _spread = json["spread"]?.asInt()
                ?: throw ProgrammingException("Missing spread.\n" + json.toString(WriterConfig.PRETTY_PRINT))

        } catch (e: IssueInTransferException) {
            // TODO: cleanup
            if (e.json.isPresent && e.json.get().asObject().getString("detail", null) == "Not found.") {
                Areas.areas.remove(this.ID)
                throw NoSuchEntityException("This Area was deleted server-side.", this)
            } else
                throw RuntimeException("An unforeseen error happened while updating an Area.", e)
        }

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val area = other as Area
        return reputation == area.reputation
            && spread == area.spread
            && ID == area.ID
    }

    override fun hashCode(): Int {
        return Objects.hash(ID)
    }

    override fun toString(): String {
        return "Area{" + "id='" + ID + '\''.toString() +
            ", reputation=" + _reputation +
            ", spread=" + _spread +
            '}'.toString()
    }

    /**
     * Gets the post with the given ID from the cache, or from the server.
     *
     * If the post is cached, this method will return in constant time and launch an update job in a new thread if
     * needed; if the post is not cached, this method will request it from the server in the current thread and only
     * return after parsing the result.
     *
     * @param id the ID of the post
     * @return The post that corresponds to the given ID, or an empty optional if no such post exist.
     */
    fun post(id: Long): Post? {
        val post = cachedPost(id) ?: Post(id, this.ID)

        // If there is no post in the cache, stall & query server
        try {
            if (post.isNew) {
                posts[id] = post
                post.update() // in this thread
            }

        } catch (e: NoSuchEntityException) {
            return null // this post doesn't exists server-side

        } catch (e: Request.CantConnectException) {
            Internal.throwCantConnect(e)
            return null
        }

        // If there is an expired post in the cache
        if (!post.isValid)
            Internal.submitUpdate(post) // in a new thread

        post.use()

        return post
    }

    /**
     * Gets the post with the given ID from the cache. This method will not attempt any call to the API, and therefore
     * executes in constant time.
     *
     * @param id the ID of the post
     * @return The post that corresponds to the given ID, or null if that ID is not in the cache.
     */
    fun cachedPost(id: Long): Post? = posts[id]

    /**
     * Clears the Draft Cache and loads it again. Any unsaved Draft will be lost.
     *
     * @throws Request.CantConnectException if the API cannot connect to the server.
     */
    @Throws(Request.CantConnectException::class)
    fun loadDrafts() = try {
        drafts.clear()
        Request(GET, "/areas/$ID/drafts/")
            .addToken(Internal.token())
            .getJsonObject()["results"].asArray().asSequence()
            .map { it as JsonObject }
            .map { it["id"]?.asString() }
            .filterNotNull()
            .map { Draft(it.toLong(), ID) }
            .forEach { Internal.submitUpdate(it); this.cachedDraft(it) }
    } catch (e: IssueInTransferException) {
        e.printStackTrace()
        //TODO: T262
    }

    /**
     * Adds a Draft to the cache.
     *
     * Note that this method does not save the Draft server-side. If this is what you want to do, see
     * [draft].
     *
     * @param draft the Draft
     */
    fun cachedDraft(draft: Draft) {
        drafts[draft.ID()] = draft
    }

    /**
     * Removes a Draft from the cache.
     *
     * Note that this method does not delete the Draft from the server's data. If this is what you want to do,
     * see [Draft.delete].
     *
     * @param draft the draft that should be removed from the cache.
     * @see MutableMap.remove
     */
    fun removeCached(draft: Draft) {
        drafts.remove(draft.ID())
    }

    /**
     * Creates a new Draft, that will later be converted to a Post.
     *
     * This method is a wrapper around [Draft] to enable cleaner code without touching to IDs.
     *
     * @return A new Draft.
     */
    fun draft(): Draft {
        return Draft(this.ID)
    }

    /**
     * Queries the server for the list of posts that are owned by the logged-in user. Note that the posts themselves
     * aren't loaded, only their ID. This method will save the list internally, but not return it.
     *
     * @throws Request.CantConnectException if the lib cannot reach the server
     * @see ownPosts
     */
    @Throws(Request.CantConnectException::class)
    fun loadOwnPosts() {
        try {
            ownPostsIDs = Request(GET, "/areas/$ID/own/")
                .addToken(Internal.token())
                .getJsonObject()["results"].asArray().asSequence()
                .map { it as JsonObject }
                .map { it["id"] }
                .filterNotNull()
                .map { it.asString().toLong() }
                .toList()

        } catch (e: IssueInTransferException) {
            throw ProgrammingException("This request shouldn't fail.", e)
        }
    }

    /**
     * The posts owned by this user.
     *
     * Note that you need to load them first, using [loadOwnPosts], otherwise, this method will return an
     * empty Stream.
     *
     * This method internally translates the stored list of IDs to a list of posts. During this process, posts are
     * queried using [post]; see its documentation for more information about the use of threads.
     *
     * @return A Stream of the posts the user owns.
     */
    fun ownPosts(): List<Post> {
        return ownPostsIDs.asSequence()
            .map { post(it) }
            .filterNotNull()
            .toList()
    }

}
