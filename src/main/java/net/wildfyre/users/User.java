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

package net.wildfyre.users;

import com.eclipsesource.json.JsonObject;
import net.wildfyre.descriptors.CacheManager;
import net.wildfyre.descriptors.Descriptor;
import net.wildfyre.descriptors.NoSuchEntityException;
import net.wildfyre.http.IssueInTransferException;
import net.wildfyre.http.Request;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import static net.wildfyre.http.Method.GET;

public class User extends Descriptor {

    protected final int ID;

    //region Fields

    protected String name = "";
    protected String avatar = "";
    protected String bio = "";
    protected boolean isBanned;

    //endregion
    //region Constructors

    User(int id) {
        this.ID = id;
    }

    //endregion
    //region Updating

    @Override
    public void update() throws NoSuchEntityException, Request.CantConnectException {
        if(!Users.getCached(this.ID).isPresent()) {
            System.err.println("The user " + ID + " is not even in the cache, aborting update early.");
            return;
        }

        try {
            JsonObject values = new Request(GET, "/users/" + ID + "/")
                .get()
                .asObject();

            // Use the old value as default value: if nothing is specified, keep the old value
            name =      values.getString("name", name);
            //TODO fix until T235
            avatar =    values.get("avatar").isString() ? values.getString("avatar", avatar) : null;
            bio =       values.getString("bio", bio);
            isBanned =  values.getBoolean("banned", isBanned);

            int user =  values.getInt("user", ID);
            if(user != ID)
                throw new RuntimeException("The ID has changed! " + ID + " -> " + user);

            this.use();

        } catch (IssueInTransferException e) {
            if(e.getJson().isPresent())
                if(e.getJson().get().asObject().getString("detail", null).equals("Not found.")) {
                    Users.users.remove(this.ID);
                    throw new NoSuchEntityException("The requested user does not exist!", this);
                }
        }
    }

    /**
     * Creates a new User object.
     * @param id the ID of the user
     * @return A new User object.
     */
    static User create(int id){
        return Users.isMyID(id) ? new LoggedUser(id) : new User(id);
    }

    @Override
    public CacheManager cacheManager() {
        return Users.cacheManager();
    }

    //endregion
    //region Getters

    /**
     * Can this user be edited?
     * <p>This method will always return {@code false} for a {@link User}, and will always return {@code true} for a
     * {@link LoggedUser}</p>
     * @return {@code true} if the user's information can edited.
     * @see #asLogged() Get this object as a LoggedUser
     */
    public boolean canEdit(){
        return false;
    }

    /**
     * This object, as a LoggedUser.
     * @return This object, as a LoggedUser.
     * @throws ClassCastException if this object cannot be converted to a LoggedUser.
     * @see #canEdit() Can this object be converted to a LoggedUser?
     */
    public final LoggedUser asLogged() throws ClassCastException {
        this.use();

        return (LoggedUser) this;
    }

    /**
     * The ID of this user.
     * @return The ID of this user, a positive integer.
     */
    public int getID() {
        this.use();

        return ID;
    }

    /**
     * The avatar of this user: the raw URL written in a String object, as specified by the server.
     * @return The avatar of this user.
     */
    public Optional<String> avatar() {
        this.use();

        return Optional.ofNullable(avatar);
    }

    /**
     * The avatar of this user: a URL object is created and points to the location of the avatar picture.
     * @return The avatar of this user.
     */
    public Optional<URL> avatarUrl() {
        this.use();

        try {
            if(avatar != null)
                return Optional.of(new URL(avatar));
            else
                return Optional.empty();

        } catch (MalformedURLException e) {
            throw new RuntimeException("Since URLs are sent by the server, they shouldn't be malformed...", e);
        }
    }

    /**
     * The bio of this user.
     * @return The bio of this user.
     */
    public String bio() {
        this.use();

        return bio;
    }

    /**
     * Is this user banned?
     * @return {@code true} if this user is banned.
     */
    public boolean isBanned() {
        this.use();

        return isBanned;
    }

    /**
     * The name of this user.
     * @return The name of this user.
     */
    public String name() {
        this.use();

        return name;
    }

    //endregion
    //region Generated

    @Override // Generated by IntelliJ
    public String toString() {
        return "User{" + "ID=" + ID +
            ", name='" + name + '\'' +
            ", avatar='" + avatar + '\'' +
            ", bio='" + bio + '\'' +
            ", isBanned=" + isBanned +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return ID == user.ID &&
            isBanned == user.isBanned &&
            Objects.equals(name, user.name) &&
            Objects.equals(avatar, user.avatar) &&
            Objects.equals(bio, user.bio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ID);
    }

    //endregion

}
