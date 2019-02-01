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

package net.wildfyre.posts;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import net.wildfyre.areas.Area;
import net.wildfyre.areas.Areas;
import net.wildfyre.users.User;
import net.wildfyre.users.Users;
import net.wildfyre.utils.InvalidJsonException;
import net.wildfyre.utils.ProgrammingException;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

import static net.wildfyre.utils.InvalidJsonException.optionalField;
import static net.wildfyre.utils.InvalidJsonException.requireField;

public class Comment {

    //region Attributes

    private int ID;
    private ZonedDateTime created;
    private String text;
    private String imageURL;

    private String areaID; // needed to remove/edit a comment
    private long postID;    // same
    private int authorID;

    //endregion
    //region Constructor

    Comment(Post parent, JsonObject json) {
        if(parent == null)
            throw new NullPointerException("The parent object should not be null.");
        areaID = parent.areaID;
        postID = parent.postID;

        try {
            update(json);

        } catch (InvalidJsonException e) {
            throw new IllegalArgumentException("There was an issue in JSON data.", e);
        }
    }

    //endregion
    //region Updates

    void update(JsonObject json) throws InvalidJsonException {
        ID = requireField(json, "id").asInt();
        authorID = requireField(requireField(json, "author").asObject(), "user").asInt();
        created = ZonedDateTime.parse(requireField(json, "created").asString());
        text = requireField(json, "text").asString();
        imageURL = optionalField(json, "image").orElse(Json.value("")).asString(); //TODO: See T258, T262
    }

    //endregion
    //region Getters

    /**
     * The ID of this comment.
     *
     * <p>Note that this API is crafted so no ID is ever needed, this method is only given in the case where you really
     * need it.</p>
     *
     * @return The ID of this comment.
     */
    public int ID(){
        return ID;
    }

    /**
     * When was this comment created?
     *
     * @return The timestamp of this comment's creation.
     */
    public ZonedDateTime created(){
        return created;
    }

    /**
     * The text of this comment.
     *
     * @return The text of this comment.
     */
    public String text(){
        return text;
    }

    /**
     * The URL to the image of this comment.
     *
     * @return The image of this comment.
     */
    public Optional<String> imageURL(){
        return Optional.ofNullable(imageURL);
    }

    /**
     * The Area in which this comment's Post was created.
     *
     * @return The Area.
     */
    public Area area(){
        return Areas.INSTANCE.get(areaID)
            .orElseThrow(RuntimeException::new); // This is not possible: Area doesn't exist
    }

    /**
     * The Post in which this comment was created.
     *
     * @return The Post in which this comment exists.
     */
    public Post post(){
        Post tmp = area().post(postID);
        if (tmp != null)
            return tmp;
        else
            throw new ProgrammingException("The post must exist.");
    }

    /**
     * The user who created this comment.
     *
     * @return The User who created this comment.
     */
    public User author(){
        return Users.get(authorID)
            .orElseThrow(RuntimeException::new); // This is not possible: Author doesn't exist
    }

    //endregion
    //region Generated

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comment comment = (Comment) o;
        return ID == comment.ID &&
            postID == comment.postID &&
            authorID == comment.authorID &&
            Objects.equals(created, comment.created) &&
            Objects.equals(text, comment.text) &&
            Objects.equals(imageURL, comment.imageURL) &&
            Objects.equals(areaID, comment.areaID);
    }

    @Override
    public int hashCode() {
        return ID;
    }

    @Override
    public String toString() {
        return "Comment{" +
            "ID=" + ID +
            ", created=" + created +
            ", text='" + text + '\'' +
            ", imageURL='" + imageURL + '\'' +
            ", areaID='" + areaID + '\'' +
            ", postID=" + postID +
            ", authorID=" + authorID +
            '}';
    }

    //endregion

}
