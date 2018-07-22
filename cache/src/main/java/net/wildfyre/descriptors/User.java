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

package net.wildfyre.descriptors;

import com.eclipsesource.json.JsonObject;
import net.wildfyre.http.IssueInTransferException;
import net.wildfyre.http.Request;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

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
    public void update() {
        try {
            JsonObject values = new Request(GET, "/users/" + ID + "/")
                .get()
                .asObject();

            // Use the old value as default value: if nothing is specified, keep the old value
            name =      values.getString("name", name);
            avatar =    values.getString("avatar", avatar);
            bio =       values.getString("bio", bio);
            isBanned =  values.getBoolean("banned", isBanned);

            int user =  values.getInt("user", ID);
            if(user != ID)
                throw new RuntimeException("The ID has changed! " + ID + " -> " + user);

            this.use();

        } catch (IssueInTransferException | Request.CantConnectException e) {
            // TODO: Better exception handling
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a specific user by ID. Here are some details on how this method works:
     * <p>If the user is NOT cached, the method stalls and queries the server. If the user is cached, the method returns
     * the cached user immediately. If the user is cached but has expired, the method immediately returns the expired
     * version and launches a refresh job that will be executed concurrently.</p>
     * <p>You can know if the user you got is being refreshed or not by checking its {@link User#isValid()} method.</p>
     * @param id the ID of the user
     * @return The requested user.
     */
    public static User query(int id){
        User user = Internal
            .getCachedUser(id)
            .orElseGet(() -> User.create(id));

        // There is no user in the cache, stall & query server
        if(user.isNew())
            user.update(); // in this thread

        // There is a user in the cache, but it's expired
        if(!user.isValid())
            Internal.submit(user::update); // in a new thread

        user.use();

        return user;
    }

    /**
     * Creates a new User object.
     * @param id the ID of the user
     * @return A new User object.
     */
    static User create(int id){
        return Internal.isMyId(id) ? new LoggedUser(id) : new User(id);
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
    public String getAvatar() {
        this.use();

        return avatar;
    }

    /**
     * The avatar of this user: a URL object is created and points to the location of the avatar picture.
     * @return The avatar of this user.
     */
    public URL getAvatarUrl() {
        this.use();

        try {
            return new URL(avatar);

        } catch (MalformedURLException e) {
            throw new RuntimeException("Since URLs are sent by the server, they shouldn't be malformed...", e);
        }
    }

    /**
     * The bio of this user.
     * @return The bio of this user.
     */
    public String getBio() {
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
    public String getName() {
        this.use();

        return name;
    }

    //endregion
    //region Generated

    @Override // Generated by IntelliJ
    public String toString() {
        final StringBuilder sb = new StringBuilder("User{");
        sb.append("ID=").append(ID);
        sb.append(", name='").append(name).append('\'');
        sb.append(", avatar='").append(avatar).append('\'');
        sb.append(", bio='").append(bio).append('\'');
        sb.append(", isBanned=").append(isBanned);
        sb.append('}');
        return sb.toString();
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
