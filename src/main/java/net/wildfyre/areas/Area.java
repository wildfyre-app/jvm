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

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import net.wildfyre.api.Internal;
import net.wildfyre.descriptors.CacheManager;
import net.wildfyre.descriptors.Descriptor;
import net.wildfyre.descriptors.NoSuchEntityException;
import net.wildfyre.http.IssueInTransferException;
import net.wildfyre.http.Request;
import net.wildfyre.posts.Draft;
import net.wildfyre.posts.Post;
import net.wildfyre.utils.LazyMap;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.wildfyre.http.Method.GET;

/**
 * This class represents an Area.
 * <p>Areas are a way Posts are 'grouped'.</p>
 * <p>Each user's spread and reputation is different from one Area to another. The reputation goes up and down when you
 * do certain actions. The spread determines how many people can see your Posts, it cannot be lower than 4.</p>
 */
public class Area extends Descriptor {

    //region Attributes

    private final String id;

    private int reputation;
    private int spread;

    //endregion
    //region Constructors

    /**
     * Creates an Area with only its id.
     * @param name the id of the Area.
     */
    Area(String name){
        if(name == null)
            throw new NullPointerException("The parameter 'name' shouldn't be null.");

        this.id = name;
    }

    //endregion
    //region Getters & Setters

    /**
     * The ID of this Area, which looks like its name in all lowercase characters.
     * <p>This API is written so you never *need* to use IDs; so if you are not sure why this exists, you can safely
     * ignore it.</p>
     * @return The ID of this Area.
     */
    public String ID(){
        return id;
    }

    /**
     * The display name of the Area.
     *
     * <p>In future versions, the display name of the Area will be handled by the server. This has not been implemented
     * yet, in the meantime, we provide a method that takes {@link #ID() the ID} and adds an uppercase letter to the
     * beginning of each word. Since this method is temporary, we make no claim that it is performance-efficient.</p>
     *
     * @return The display name of the Area.
     */
    public String name() {
        char[] chars = id.toCharArray();

        boolean nextUpper = true;
        for(int i = 0; i < chars.length; i++){
            if(nextUpper)
                chars[i] = Character.toUpperCase(chars[i]);
            nextUpper = Character.isSpaceChar(chars[i]);
        }

        return String.valueOf(chars);
    }

    /**
     * The reputation of the logged-in user, in the current area.
     * <p>The reputation increases and decreases depending on your overall actions on WildFyre.
     * You cannot control it.</p>
     * @return Your reputation.
     * @see #spread() Spread
     */
    public OptionalInt reputation(){
        return reputation != -1 ? OptionalInt.of(reputation) : OptionalInt.empty();
    }

    /**
     * The spread of the logged-in user, in the current area.
     * <p>Your spread is related to your reputation, but cannot be inferior to 4. You cannot control it.</p>
     * @return Your spread.
     * @see #reputation() Reputation
     */
    public OptionalInt spread(){
        return spread != -1 ? OptionalInt.of(spread) : OptionalInt.empty();
    }

    @Override
    public CacheManager cacheManager() {
        return Areas.cacheManager();
    }

    //endregion
    //region Update

    @Override
    public void update() throws Request.CantConnectException, NoSuchEntityException {
        try {
            JsonObject json = new Request(GET, "/areas/" + id + "/rep/")
                .addToken(Internal.token())
                .getJson()
                .asObject();

            reputation = json.getInt("reputation", -1);
            spread = json.getInt("spread", -1);

            if(reputation == -1 || spread == -1)
                throw new RuntimeException("Missing either the reputation or the spread:\n"
                    + json.toString(WriterConfig.PRETTY_PRINT));

        } catch (IssueInTransferException e) {
            if(e.getJson().isPresent()
                && e.getJson().get().asObject().getString("detail", null).equals("Not found.")) {
                Areas.areas.remove(this.id);
                throw new NoSuchEntityException("This Area was deleted server-side.", this);
            } else
                throw new RuntimeException("An unforeseen error happened while updating an Area.", e);
        }
    }

    //endregion
    //region Generated

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Area area = (Area) o;
        return reputation == area.reputation &&
            spread == area.spread &&
            Objects.equals(id, area.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Area{" + "id='" + id + '\'' +
            ", reputation=" + reputation +
            ", spread=" + spread +
            '}';
    }

    //endregion
    //region Posts
    //region All posts

    private Map<Long, Post> posts = new LazyMap<>();

    /**
     * Gets the post with the given ID from the cache, or from the server.
     *
     * <p>If the post is cached, this method will return in constant time and launch an update job in a new thread if
     * needed; if the post is not cached, this method will request it from the server in the current thread and only
     * return after parsing the result.</p>
     *
     * @param id the ID of the post
     * @return The post that corresponds to the given ID, or an empty optional if no such post exist.
     */
    public Optional<Post> post(long id){
        Post post = cachedPost(id)
            .orElseGet(() -> new Post(id, this.id));

        // If there is no post in the cache, stall & query server
        try{
            if(post.isNew()){
                posts.put(id, post);
                post.update(); // in this thread
            }

        } catch (NoSuchEntityException e) {
            return Optional.empty(); // this post doesn't exists server-side

        } catch (Request.CantConnectException e) {
            Internal.throwCantConnect(e);
            return Optional.empty();
        }

        // If there is an expired post in the cache
        if(!post.isValid())
            Internal.submitUpdate(post); // in a new thread

        post.use();

        return Optional.of(post);
    }

    /**
     * Gets the post with the given ID from the cache. This method will not attempt any call to the API, and therefore
     * executes in constant time.
     *
     * @param id the ID of the post
     * @return The post that corresponds to the given ID, or an empty optional if that ID is not in the cache.
     */
    public Optional<Post> cachedPost(long id){
        return Optional.ofNullable(posts.get(id));
    }

    //endregion
    //region Next Posts
    //TODO: See T264
    //endregion
    //region Drafts

    Map<Long, Draft> drafts = new LazyMap<>();

    /**
     * Clears the Draft Cache and loads it again. Any unsaved Draft will be lost.
     *
     * @throws Request.CantConnectException if the API cannot connect to the server.
     */
    public void loadDrafts() throws Request.CantConnectException {
        try {
            drafts.clear();
            new Request(GET, "/areas/" + id + "/drafts/")
                .addToken(Internal.token())
                .getJson().asObject()
                .get("results").asArray()
                .values().stream()
                .map(j -> j.asObject().getString("id", "null"))
                .filter(j -> !j.equals("null"))
                .mapToLong(Long::valueOf)
                .mapToObj(i -> new Draft(i, id))
                .peek(Internal::submitUpdate)
                .forEach(this::cachedDraft);

        } catch (IssueInTransferException e) {
            e.printStackTrace();
            //TODO: T262
        }
    }

    /**
     * Adds a Draft to the cache.
     *
     * <p>Note that this method does not save the Draft server-side. If this is what you want to do, see
     * {@link #draft()}.</p>
     *
     * @param draft the Draft
     */
    public void cachedDraft(Draft draft){
        drafts.put(draft.ID(), draft);
    }

    /**
     * Removes a Draft from the cache.
     *
     * <p>Note that this method does not delete the Draft from the server's data. If this is what you want to do,
     * see {@link Draft#delete()}.</p>
     *
     * @param draft the draft that should be removed from the cache.
     * @see Map#remove(Object) More details
     */
    public void removeCached(Draft draft){
        drafts.remove(draft.ID());
    }

    /**
     * Creates a new Draft, that will later be converted to a Post.
     *
     * <p>This method is a wrapper around {@link Draft#Draft(String)} to enable cleaner code without touching to IDs.
     * </p>
     *
     * @return A new Draft.
     */
    public Draft draft(){
        return new Draft(this.id);
    }

    //endregion
    //region Own Posts

    private List<Integer> ownPostsIDs = Collections.emptyList();

    /**
     * Queries the server for the list of posts that are owned by the logged-in user. Note that the posts themselves
     * aren't loaded, only their ID. This method will save the list internally, but not return it.
     *
     * @throws Request.CantConnectException if the lib cannot reach the server
     * @see #ownPosts() The list of posts owned by the logged-in user, as a Stream
     * @see #ownPostsList() The list of posts owned by the logged-in user, as a List
     */
    public void loadOwnPosts() throws Request.CantConnectException {
        try {
            ownPostsIDs = new Request(GET, "/areas/" + id + "/own/")
                .addToken(Internal.token())
                .getJson().asObject()
                .get("results").asArray()
                .values()
                .stream()
                .map(j -> j.asObject().get("id"))
                .filter(j -> !j.isNull())
                .map(j -> Integer.valueOf(j.asString()))
                .collect(Collectors.toList());

        } catch (IssueInTransferException e) {
            throw new RuntimeException("Nothing should go wrong with this request.", e);
        }
    }

    /**
     * The posts owned by this user.
     *
     * <p>Note that you need to load them first, using {@link #loadOwnPosts()}, otherwise, this method will return an
     * empty list.</p>
     *
     * <p>This method internally translates the stored list of IDs to a list of posts. During this process, posts are
     * queried using {@link #post(long)}; see its documentation for more information about the use of threads.</p>
     *
     * @return The list of posts owned by this user.
     * @see #ownPosts() This method as a Stream, for better performances.
     */
    public List<Post> ownPostsList(){
        return ownPostsIDs.isEmpty() ? Collections.emptyList() : ownPosts().collect(Collectors.toList());
    }

    /**
     * The posts owned by this user.
     *
     * <p>Note that you need to load them first, using {@link #loadOwnPosts()}<, otherwise, this method will return an
     * empty Stream./p>
     *
     * <p>This method internally translates the stored list of IDs to a list of posts. During this process, posts are
     * queried using {@link #post(long)}; see its documentation for more information about the use of threads.</p>
     *
     * @return A Stream of the posts the user owns.
     * @see #ownPostsList() This method as a List, for compatibility with the "traditionnal way".
     */
    public Stream<Post> ownPosts(){
        return ownPostsIDs.stream()
            .map(this::post)
            .filter(Optional::isPresent)
            .map(Optional::get); // IDs that correspond to no posts are ignored
    }

    //endregion
    //endregion

}
