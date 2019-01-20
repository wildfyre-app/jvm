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

package net.wildfyre.posts;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import net.wildfyre.areas.Area;
import net.wildfyre.areas.Areas;
import net.wildfyre.descriptors.Descriptor;
import net.wildfyre.users.User;
import net.wildfyre.users.Users;
import net.wildfyre.utils.InvalidJsonException;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Long.parseLong;
import static net.wildfyre.utils.InvalidJsonException.optionalField;
import static net.wildfyre.utils.InvalidJsonException.requireField;

/**
 * This class is written as an internal dataholder of the API. It is not part of the API.
 */
abstract class PostData extends Descriptor {

    //region Attributes

    boolean isAnonymous;
    boolean hasSubscribed;
    ZonedDateTime created;
    boolean isActive;

    String text;
    String imageURL;
    String[] additionalImages;

    int authorID;
    String areaID;
    long postID;

    List<Comment> comments;

    //endregion
    //region Constructors

    /**
     * Creates a PostData object with default values.
     */
    PostData(){
        isAnonymous = false;
        hasSubscribed = true;
        created = ZonedDateTime.now(ZoneId.ofOffset("UTC", ZoneOffset.UTC));
        isActive = true;
        text = null;
        imageURL = null;
        additionalImages = new String[0];
        authorID = Users.myID().orElseThrow(() -> new NullPointerException("The creation of this object"+
            "requires that the library is initialized, and that the ID of the user is known."));
        areaID = null;
        postID = -1;
        comments = Collections.emptyList();
    }

    /**
     * Creates a PostData object by copying an other existing object.
     * @param other another PostData object, that is copied into this object.
     */
    PostData(PostData other){
        update(other);
    }

    //endregion
    //region Update

    /**
     * Updates this object from some JSON data.
     * @param json the JSON data
     * @throws InvalidJsonException If the JSON data is incorrect.
     */
    void update(JsonObject json) throws InvalidJsonException {
        postID = parseLong(requireField(json, "id").asString()); //TODO: hotfix before T262
        if(!json.get("author").isNull())
            authorID = requireField(requireField(json, "author").asObject(), "user").asInt(); // UNTIL T256
        else
            authorID = -1;
        isAnonymous = requireField(json, "anonym").asBoolean();
        hasSubscribed = requireField(json, "subscribed").asBoolean();
        created = ZonedDateTime.parse(requireField(json, "created").asString());
        isActive = requireField(json, "active").asBoolean();
        text = requireField(json, "text").asString();
        //TODO: hotfix before T262
        imageURL = optionalField(json, "image").orElse(Json.value("")).asString();

        JsonArray imgs = requireField(json, "additional_images").asArray();
        additionalImages = new String[imgs.size()];
        for(int i = 0; i < imgs.size(); i++)
            additionalImages[i] = imgs.get(i).asString();

        if(Post.class.isAssignableFrom(this.getClass())) { // Am I a subclass of Post? Comments do not work with Drafts
            JsonArray cmts = requireField(json, "comments").asArray();
            comments = cmts.values()
                .stream()
                .map(j -> new Comment((Post) this, j.asObject()))
                .collect(Collectors.toList());
        } else comments = Collections.emptyList(); // No comments for Drafts

        // Only field that is not updated is the areaID (it's not in the JSON).
    }

    /**
     * Updates this object has a copy of another object.
     * @param other The other object, that is copied into this object.
     */
    private void update(PostData other){
        isAnonymous = other.isAnonymous;
        hasSubscribed = other.hasSubscribed;
        created = other.created;
        isActive = other.isActive;
        text = other.text;
        imageURL = other.imageURL;
        additionalImages = other.additionalImages;
        authorID = other.authorID;
        areaID = other.areaID;
        postID = other.postID;
        comments = new ArrayList<>(comments);
    }

    //endregion
    //region Getters

    /**
     * Is this post anonymous?
     *
     * <p>Note that in the case where a not-anonymous post's author is deleted, the post is still not anonymous, but
     * doesn't have an author either.</p>
     *
     * @return {@code true} if the post is anonymous.
     * @see #author() Get the author of this message.
     * @see #isAuthorDeleted() Was the author of this post deleted?
     */
    public boolean isAnonymous(){
        return isAnonymous;
    }

    /**
     * Is the logged-in user subscribed to this post?
     *
     * @return {@code true} if the logged-in user subscribed to this post.
     */
    public boolean hasSubscribed(){
        return hasSubscribed;
    }

    /**
     * Was the author of this post deleted?
     *
     * <p>This method is a simple helper rather than a getter for some server-provided data. It is a shortcut for:
     * <pre>{@code !author().isPresent() && !isAnonymous();}</pre>
     * </p>
     *
     * @return {@code true} if the author of this post was deleted.
     * @see #isAnonymous() Is this post anonymous?
     * @see #author() The author of this post.
     */
    public boolean isAuthorDeleted(){
        return !Users.get(authorID).isPresent() && !isAnonymous;
    }

    /**
     * The date and time at which this post was created, in the UTC timezone.
     *
     * @return The date and time at which this post was created.
     * @see #createdLocalTime() In the local timezone
     */
    public ZonedDateTime created(){
        return created;
    }

    /**
     * The date and time at which this post was created, in the local timezone.
     *
     * The local timezone is acquired using {@link ZoneId#systemDefault()}, if you are searching for an other timezone,
     * use {@link #created()} and {@link ZonedDateTime#withZoneSameInstant(ZoneId)}.
     *
     * @return The date and time at which this post was created.
     * @see #created() In the UTC timezone
     */
    public ZonedDateTime createdLocalTime(){
        return created.withZoneSameInstant(ZoneId.systemDefault());
    }

    /**
     * Is this post active?
     *
     * <p>An active post is a post that continues to spread to new users.</p>
     *
     * @return {@code true} if this post is active.
     */
    public boolean isActive(){
        return isActive;
    }

    /**
     * The text of this post, in MarkDown.
     *
     * @return The text of this post.
     */
    public String text(){
        return text;
    }

    //TODO: Add support for images, see T258

    /**
     * The author of this post.
     *
     * <p>There are two cases where there may not be an author: the author chose this post post to be anonymous, or the
     * original author was deleted since this post's creation.</p>
     *
     * @return The author of this post, if any.
     * @see #isAnonymous() Is this post anonymous?
     * @see #isAuthorDeleted() Was the author of this post deleted?
     */
    public Optional<User> author(){
        return authorID == -1 ? Optional.empty()
            : Optional.of(Users.get(authorID)
            .orElseThrow(() -> new RuntimeException("Couldn't find the author of this post!\n"
            + toString())));
    }

    /**
     * The area in which is post was published.
     *
     * @return The area in which the post was published.
     */
    public Area area(){
        return Areas.INSTANCE.get(areaID)
            .orElseThrow(() -> new RuntimeException("Couldn't find the area in which this post was created!\n"
            + toString()));
    }

    /**
     * The ID of this post.
     *
     * <p>This API is crafted so that the end-user doesn't need to use IDs. This method is provided for the rare case
     * where you need the ID for customized handling of the cache.</p>
     *
     * @return The ID of this post.
     */
    public long ID(){
        return postID;
    }

    /**
     * The comments on this post, as a List.
     *
     * <p>The returned list is read-only and provided by {@link Collections#unmodifiableList(List)}.</p>
     *
     * @return A read-only list of the comments in this post.
     * @see #comments() We recommend using Streams
     */
    public List<Comment> commentsList(){
        return Collections.unmodifiableList(comments);
    }

    /**
     * The comments on this post.
     *
     * @return The comments on this post.
     * @see #commentsList() Get the comments in a List.
     */
    public Stream<Comment> comments(){
        return comments.stream();
    }

    //endregion
    //region Setters Interface

    /**
     * Contains the Setters for PostData. This is need because not all PostData children are modifiable: for example,
     * a Post cannot be modified.
     *
     * <p>The architecture aim here, is to decrease code duplication. Post inherits PostData but not PostDataSetters,
     * as a Post cannot be modified. Draft inherits both, as Drafts can be modified. In the future, when OwnPosts are
     * implemented, they will inherit both too.</p>
     *
     * <p>The reason this is an interface rather than an abstract class is because it will implemented completely
     * differently for Drafts and OwnPosts.</p>
     *
     * @param <P> The PostData that is modified, to allow fluent API.
     */
    interface Setters<P extends PostData> {

        /**
         * Make this object anonymous or not anonymous.
         * @param isAnonymous {@code true} to make this object anonymous, {@code false} to make it not-anonymous.
         * @return This object itself, to allow method-chaining.
         */
        P setIsAnonymous(boolean isAnonymous);

        /**
         * Subscribes the logged-in user to this post.
         * @return This object itself, to allow method-chaining.
         */
        P subscribe();

        /**
         * Unsubscribes the logged-in user from this post.
         * @return This object itself, to allow method-chaining.
         */
        P unsubscribe();

        /**
         * Sets the text of this object.
         * @param newText the new text of this object.
         * @return This object itself, to allow method-chaining.
         */
        P setText(String newText);

        //TODO: Add support for images, see T258

        /**
         * Deletes this object, from both the server and the cache.
         */
        void delete();

    }

    //endregion
    //region Generated & serialization

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostData postData = (PostData) o;
        return isAnonymous == postData.isAnonymous &&
            hasSubscribed == postData.hasSubscribed &&
            isActive == postData.isActive &&
            authorID == postData.authorID &&
            postID == postData.postID &&
            Objects.equals(created, postData.created) &&
            Objects.equals(text, postData.text) &&
            Objects.equals(imageURL, postData.imageURL) &&
            Arrays.equals(additionalImages, postData.additionalImages) &&
            Objects.equals(areaID, postData.areaID) &&
            Objects.equals(comments, postData.comments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(areaID, postID);
    }

    @Override
    public String toString() {
        return "PostData{" + "isAnonymous=" + isAnonymous +
            ", hasSubscribed=" + hasSubscribed +
            ", created=" + created +
            ", isActive=" + isActive +
            ", text='" + text + '\'' +
            ", imageURL='" + imageURL + '\'' +
            ", additionalImages=" + Arrays.toString(additionalImages) +
            ", authorID=" + authorID +
            ", areaID='" + areaID + '\'' +
            ", postID=" + postID +
            ", comments=" + comments +
            '}';
    }

    /**
     * Generates the JSON String needed by the server to understand this object. Some elements are excluded:
     *
     * <ul>
     *     <li>The ID of this object</li>
     *     <li>The comments</li>
     *     <li>The author</li>
     *     <li>The activity of this post (active or not)</li>
     * </ul>
     *
     * @return This object, written in JSON.
     */
    protected final JsonObject toJsonSimple() {
        JsonArray images = new JsonArray();
        for(String s : additionalImages)
            images.add(s);

        return new JsonObject()
            .add("anonym", isAnonymous)
            .add("subscribed", hasSubscribed)
            .add("created", created.format(DateTimeFormatter.ISO_DATE_TIME))
            .add("text", text)
            .add("image", imageURL)
            .add("additional_images", images);
    }

    //endregion

}
